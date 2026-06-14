#!/usr/bin/env python3
"""
Generate MongoDB campaign and organization documents from raw Indiegogo data.

The script reads:
  data/raw/indiegogo_campaigns_raw.json

And writes:
  data/mongo/campaigns.json
  data/mongo/organization_users.json
  data/mongo/organization_dashboards.json
  data/mongo/indiegogo_generation_summary.json
"""

import argparse
import csv
import hashlib
import json
import random
import re
from datetime import datetime, timezone
from decimal import Decimal, InvalidOperation
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RAW_FILE = ROOT / "data/raw/indiegogo_campaigns_raw.json"
ADDRESSES_FILE = ROOT / "data/raw/random_addresses.txt"
CAMPAIGNS_FILE = ROOT / "data/mongo/campaigns.json"
ORGANIZATIONS_FILE = ROOT / "data/mongo/organization_users.json"
ORGANIZATION_DASHBOARDS_FILE = ROOT / "data/mongo/organization_dashboards.json"
SUMMARY_FILE = ROOT / "data/mongo/indiegogo_generation_summary.json"

PLACEHOLDER_IMAGE_URL = "https://placehold.co/800x450/png?text=Campaign"
PASSWORD_HASH = "$2a$10$l/IzdOGtXPXrbvuXhqJhIONVxWmZesXEu/qcJuuwrqAHso1AFesXy"  # password123


def write_json(path, data):
    """Write pretty JSON and create the target directory when needed."""
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def clean_text(value):
    """Normalize optional raw values into stripped strings."""
    return "" if value is None else str(value).strip()


def make_id(prefix, *values):
    """Build a stable string identifier from source values."""
    text = "|".join(str(value) for value in values)
    return f"{prefix}_{hashlib.sha1(text.encode('utf-8')).hexdigest()[:16]}"


def make_object_id(*values):
    """Build a deterministic 24-character Mongo ObjectId string."""
    text = "|".join(str(value) for value in values)
    return hashlib.sha1(text.encode("utf-8")).hexdigest()[:24]


def normalize_tags(tags):
    """Convert raw tag labels into the enum-like format used by campaigns."""
    result = []
    for tag in tags or []:
        tag = clean_text(tag).upper().replace("&", " AND ")
        tag = re.sub(r"[^A-Z0-9]+", "_", tag).strip("_")
        tag = "_" + tag if tag[:1].isdigit() else tag
        if tag and tag not in result:
            result.append(tag)
    return result


def decimal(value, default="0"):
    """Represent a numeric value in Mongo extended JSON Decimal128 format."""
    try:
        return {"$numberDecimal": format(Decimal(str(value if value not in (None, "") else default)), "f")}
    except InvalidOperation:
        return {"$numberDecimal": default}


def normalize_date(value):
    """Normalize source timestamps to UTC ISO strings accepted by Mongo import."""
    if not clean_text(value):
        return None
    value = str(value).strip()
    if value.endswith("Z"):
        value = value[:-1] + "+00:00"
    try:
        date = datetime.fromisoformat(value)
        if date.tzinfo:
            date = date.astimezone(timezone.utc).replace(tzinfo=None)
        return date.isoformat(timespec="seconds") + "Z"
    except Exception:
        return str(value).replace("Z", "")


def mongo_date(value):
    """Wrap a normalized timestamp as a Mongo extended JSON date."""
    value = normalize_date(value)
    return {"$date": value} if value else None


def load_addresses():
    """Load complete address rows used for organization locations."""
    with ADDRESSES_FILE.open(encoding="utf-8", newline="") as file:
        return [
            {"city": row[0].strip(), "street": row[1].strip(), "zip": row[2].strip()}
            for row in csv.reader(file)
            if len(row) >= 3 and row[0].strip() and row[1].strip() and row[2].strip()
        ]


def email_from_creator(creator_url_name, source_creator_id):
    """Generate a stable local email address for an organization account."""
    name = re.sub(r"[^a-z0-9]+", ".", str(creator_url_name).lower()).strip(".")
    if not name:
        name = "organization"
    return f"{name}.{source_creator_id}@indiegogo.local"


def valid_raw_campaign(raw):
    """Return true when a raw campaign has the fields required by the app."""
    source = raw.get("source") or {}
    organization = raw.get("organization") or {}
    snapshot = raw.get("organizationSnapshot") or {}

    return all((
        clean_text(source.get("projectId")),
        clean_text(source.get("projectUrlName")),
        clean_text(source.get("projectUrl")),
        clean_text(snapshot.get("name")),
        clean_text(organization.get("sourceCreatorId")),
        clean_text(organization.get("creatorUrlName")),
        clean_text(raw.get("title")),
        clean_text(raw.get("startDate")),
        clean_text(raw.get("endDate")),
        clean_text(raw.get("goalAmount")),
    ))


def build_organization(raw, organization_id, address):
    """Create one organization user_account document from raw campaign data."""
    organization = raw.get("organization") or {}
    snapshot = raw.get("organizationSnapshot") or {}
    source_creator_id = organization.get("sourceCreatorId")
    creator_url_name = clean_text(organization.get("creatorUrlName"))
    name = clean_text(snapshot.get("name"))
    legal_name = clean_text(organization.get("legalName")) or name
    now = mongo_date(datetime.now(timezone.utc).isoformat())

    return {
        "_id": organization_id,
        "_class": "xyz.nardone.aide.largescale.entity.OrganizationEntity",
        "displayName": name,
        "email": email_from_creator(creator_url_name, source_creator_id),
        "password": PASSWORD_HASH,
        "createdAt": now,
        "updatedAt": now,
        "role": {"name": "ROLE_ORGANIZATION"},
        "location": address,
        "legalName": legal_name,
        "status": "ACTIVE",
    }


def build_milestones(raw, campaign_id):
    """Convert raw milestone entries into embedded campaign milestones."""
    return [
        {
            "milestoneId": f"{campaign_id}_milestone_{index}",
            "title": clean_text(milestone.get("title")) or f"Milestone {index}",
            "targetAmount": decimal(milestone.get("targetAmount")),
            "status": clean_text(milestone.get("status")) or "PENDING",
            "verificationDate": mongo_date(milestone.get("verificationDate")),
        }
        for index, milestone in enumerate(raw.get("milestones") or [], start=1)
    ]


def build_updates(raw, campaign_id):
    """Convert raw campaign updates into embedded campaign updates."""
    return [
        {
            "updateId": f"{campaign_id}_update_{index}",
            "date": mongo_date(update.get("date")) or mongo_date(raw.get("updatedAt")),
            "title": clean_text(update.get("title")) or f"Update {index}",
            "description": clean_text(update.get("description")),
        }
        for index, update in enumerate(raw.get("updates") or [], start=1)
    ]


def build_rewards(raw, campaign_id):
    """Convert raw rewards into embedded campaign reward options."""
    return [
        {
            "rewardId": f"{campaign_id}_reward_{index}",
            "title": clean_text(reward.get("title")) or f"Reward {index}",
            "description": clean_text(reward.get("description")),
            "amount": decimal(reward.get("amount")),
        }
        for index, reward in enumerate(raw.get("availableRewards") or [], start=1)
    ]


def build_campaign(raw, campaign_id, organization_id, address):
    """Create one campaign document with the embedded projections used by reads."""
    stats = raw.get("stats") or {}
    milestones = build_milestones(raw, campaign_id)
    updates = build_updates(raw, campaign_id)
    rewards = build_rewards(raw, campaign_id)
    donations_count = int(stats.get("donationsCount") or 0)
    start_date = mongo_date(raw.get("startDate"))
    end_date = mongo_date(raw.get("endDate"))
    updated_at = mongo_date(raw.get("updatedAt")) or start_date

    return {
        "_id": {"$oid": campaign_id},
        "_class": "xyz.nardone.aide.largescale.entity.CampaignEntity",
        "organizationId": organization_id,
        "organizationName": clean_text((raw.get("organizationSnapshot") or {}).get("name")),
        "organizationCity": address["city"],
        "isOrganizationActive": True,
        "title": clean_text(raw.get("title")),
        "description": clean_text(raw.get("description")),
        "thumbnailImageUrl": clean_text(raw.get("thumbnailImageUrl")) or PLACEHOLDER_IMAGE_URL,
        "tags": normalize_tags(raw.get("tags")),
        "startDate": start_date,
        "endDate": end_date,
        "status": clean_text(raw.get("status")) or "OPEN",
        "goalAmount": decimal(raw.get("goalAmount")),
        "raisedAmount": decimal(raw.get("raisedAmount")),
        "donationsCount": donations_count,
        # The donation generator will fill these projections consistently.
        "pendingRewardsCount": 0,
        "milestonesTotalCount": len(milestones),
        "verifiedMilestonesCount": len([m for m in milestones if m["status"] == "VERIFIED"]),
        "latestDonations": [],
        "pendingRewards": [],
        "concludedDonationIds": [],
        "milestones": milestones,
        "updates": updates,
        "availableRewards": rewards,
        "createdAt": start_date,
        "updatedAt": updated_at,
    }


def build_organization_dashboard(organization_id, campaigns):
    """Create the organization dashboard document from its generated campaigns."""
    # Dashboard lists expose the most recently started campaigns first.
    campaigns = sorted(campaigns, key=lambda campaign: campaign["startDate"]["$date"], reverse=True)
    open_campaigns = [
        {
            "campaignId": campaign["_id"]["$oid"],
            "title": campaign["title"],
            "startDate": campaign["startDate"],
            "endDate": campaign["endDate"],
            "raisedAmount": campaign["raisedAmount"],
            "goalAmount": campaign["goalAmount"],
        }
        for campaign in campaigns
        if campaign["status"] == "OPEN"
    ]
    archived_campaign_ids = [
        campaign["_id"]["$oid"]
        for campaign in campaigns
        if campaign["status"] != "OPEN"
    ]

    return {
        "_id": organization_id,
        "_class": "xyz.nardone.aide.largescale.entity.OrganizationDashboardEntity",
        "openCampaigns": open_campaigns,
        "closedOrConcludedCampaignIds": archived_campaign_ids,
        "openCampaignsCount": len(open_campaigns),
    }


def main():
    """Generate Mongo seed documents from raw Indiegogo crawler output."""
    parser = argparse.ArgumentParser(description="Generate MongoDB campaign, organization, and dashboard documents.")
    parser.add_argument("--input", type=Path, default=RAW_FILE)
    args = parser.parse_args()

    raw_documents = json.loads(args.input.read_text(encoding="utf-8"))
    addresses = load_addresses()
    campaigns = []
    organizations = {}

    for raw in raw_documents:
        # Skip incomplete source records instead of generating invalid documents.
        if not valid_raw_campaign(raw):
            continue

        source = raw.get("source") or {}
        organization = raw.get("organization") or {}

        campaign_id = make_object_id(source.get("projectId"), source.get("projectUrlName"))
        organization_id = make_id("organization", organization.get("sourceCreatorId"), organization.get("creatorUrlName"))

        if organization_id not in organizations:
            organizations[organization_id] = build_organization(raw, organization_id, random.Random(organization_id).choice(addresses))

        # Campaign city must match the owning Organization city.
        campaign = build_campaign(raw, campaign_id, organization_id, organizations[organization_id]["location"])
        campaigns.append(campaign)

    # Build one dashboard per organization from the final campaign list.
    campaigns_by_organization = {
        organization_id: [campaign for campaign in campaigns if campaign["organizationId"] == organization_id]
        for organization_id in organizations
    }
    dashboards = [
        build_organization_dashboard(organization_id, organization_campaigns)
        for organization_id, organization_campaigns in campaigns_by_organization.items()
    ]

    summary = {
        "maxMilestonesInCampaign": max((len(campaign["milestones"]) for campaign in campaigns), default=0),
        "maxUpdatesInCampaign": max((len(campaign["updates"]) for campaign in campaigns), default=0),
        "maxRewardsInCampaign": max((len(campaign["availableRewards"]) for campaign in campaigns), default=0),
        "totalValidDocumentsConsidered": len(campaigns),
        "totalOrganizations": len(organizations),
        "totalOrganizationDashboards": len(dashboards),
    }

    # Persist the generated collections and a small audit summary for review.
    write_json(CAMPAIGNS_FILE, campaigns)
    write_json(ORGANIZATIONS_FILE, list(organizations.values()))
    write_json(ORGANIZATION_DASHBOARDS_FILE, dashboards)
    write_json(SUMMARY_FILE, summary)
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
