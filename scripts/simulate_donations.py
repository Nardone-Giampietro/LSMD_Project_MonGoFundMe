#!/usr/bin/env python3
"""
Simulate donations with a geometric donor-frequency model.

The script assigns synthetic donation slots to donors, generates donation
documents and writes a matplotlib chart of donor donation frequencies.
"""

import argparse
import hashlib
import json
import random
from collections import Counter, defaultdict
from datetime import datetime, timedelta
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt


ROOT = Path(__file__).resolve().parents[1]
DONORS_FILE = ROOT / "data/mongo/donor_users.json"
CAMPAIGNS_FILE = ROOT / "data/mongo/campaigns.json"
ORGANIZATION_DASHBOARDS_FILE = ROOT / "data/mongo/organization_dashboards.json"
DONATIONS_FILE = ROOT / "data/mongo/donations.json"
DONOR_DASHBOARDS_FILE = ROOT / "data/mongo/donor_dashboards.json"
CHART_FILE = ROOT / "data/mongo/donor_donation_distribution.png"
SEED = 42
REWARD_PROBABILITY = 0.30


def donation_amounts(total_amount, count, rng):
    """Split a campaign total amount into random donation amounts in cents."""
    if count == 0:
        return []

    total_cents = int((Decimal(total_amount["$numberDecimal"]) * Decimal("100")).quantize(Decimal("1"), rounding=ROUND_HALF_UP))
    remaining = total_cents - count
    weights = [rng.expovariate(1.0) for _ in range(count)]
    weight_sum = sum(weights)
    extras = [int(weight / weight_sum * remaining) for weight in weights]
    amounts = [1 + extra for extra in extras]

    missing = total_cents - sum(amounts)
    fractions = [
        (weights[index] / weight_sum * remaining - extras[index], index)
        for index in range(count)
    ]
    for _, index in sorted(fractions, reverse=True)[:missing]:
        amounts[index] += 1

    return amounts


def normalize_campaign_counts(campaigns):
    """Ensure campaigns with raised money have at least one donation slot."""
    corrected = 0
    for campaign in campaigns:
        raised_amount = Decimal(campaign["raisedAmount"]["$numberDecimal"])
        if not campaign.get("donationsCount") and raised_amount > 0:
            # Some scraped campaigns report money raised but no donor count.
            campaign["donationsCount"] = 1
            corrected += 1
    return corrected


def geometric(p, rng):
    """Sample a geometric count with a minimum value of one."""
    count = 1
    while rng.random() > p:
        count += 1
    return count


def donor_donation_counts(donors, total_donations, p, rng):
    """Assign a donation count to each donor while matching the global total."""
    counts = [geometric(p, rng) for _ in donors]
    total = sum(counts)

    # Add or remove slots until the sampled distribution exactly matches the dataset.
    while total < total_donations:
        counts[rng.randrange(len(counts))] += 1
        total += 1

    while total > total_donations:
        index = rng.randrange(len(counts))
        if counts[index] > 1:
            counts[index] -= 1
            total -= 1

    return counts


def random_donation_date(campaign, rng):
    """Pick a random donation timestamp inside the campaign date range."""
    start = datetime.fromisoformat(campaign["startDate"]["$date"].replace("Z", "+00:00"))
    end = datetime.fromisoformat(campaign["endDate"]["$date"].replace("Z", "+00:00"))
    seconds = int((end - start).total_seconds())
    if seconds <= 0:
        return {"$date": start.isoformat(timespec="seconds").replace("+00:00", "Z")}
    date = start + timedelta(seconds=rng.randint(0, seconds))
    return {"$date": date.isoformat(timespec="seconds").replace("+00:00", "Z")}


def build_donation(index, donor, campaign, amount_cents, reward, status, donated_at):
    """Create one donation document and its donor snapshot."""
    donation_id = hashlib.sha1(f"donation_{index:08d}".encode("utf-8")).hexdigest()[:24]
    location = donor.get("location") or {}
    snapshot = {
        "fullName": donor.get("displayName"),
        "email": donor.get("email"),
    }

    if reward:
        snapshot["address"] = {
            "street": location.get("street"),
            "city": location.get("city"),
            "zip": location.get("zip"),
        }

    donation = {
        "_id": {"$oid": donation_id},
        "_class": "xyz.nardone.aide.largescale.entity.DonationEntity",
        "donorId": donor["_id"],
        "organizationId": campaign["organizationId"],
        "campaignId": campaign["_id"],
        "campaignTitle": campaign["title"],
        "organizationLegalName": campaign.get("organizationName"),
        "amount": {"$numberDecimal": format(Decimal(amount_cents) / Decimal("100"), ".2f")},
        "donatedAt": donated_at,
        "status": status,
        "donorSnapshot": snapshot,
    }
    if reward:
        donation["reward"] = {
            "rewardId": reward.get("rewardId"),
            "title": reward.get("title"),
        }
    return donation


def update_campaign(campaign, donations):
    """Refresh campaign embedded donation projections from generated donations."""
    donations.sort(key=lambda donation: donation["donatedAt"]["$date"], reverse=True)
    campaign["latestDonations"] = [
        {
            "donorName": donation["donorSnapshot"]["fullName"],
            "amount": donation["amount"],
            "donatedAt": donation["donatedAt"],
        }
        for donation in donations[:10]
    ]
    campaign["pendingRewards"] = [
        {
            "donationId": donation["_id"]["$oid"],
            "donorName": donation["donorSnapshot"]["fullName"],
            "amount": donation["amount"],
            "donatedAt": donation["donatedAt"],
        }
        for donation in donations
        if donation["status"] == "PENDING"
    ]
    campaign["concludedDonationIds"] = [
        donation["_id"]["$oid"]
        for donation in donations
        if donation["status"] == "CONCLUDED"
    ]
    campaign["pendingRewardsCount"] = len(campaign["pendingRewards"])


def build_donor_dashboards(donors, donations):
    """Create donor dashboard documents ordered by most recent donation."""
    donations_by_donor = defaultdict(list)
    for donation in donations:
        donations_by_donor[donation["donorId"]].append({
            "donationId": donation["_id"]["$oid"],
            "campaignId": donation["campaignId"]["$oid"],
            "campaignTitle": donation["campaignTitle"],
            "donatedAt": donation["donatedAt"],
            "amount": donation["amount"],
            "status": donation["status"],
            "_class": "xyz.nardone.aide.largescale.entity.embedded.dashboard.DonorDonationSummaryEntity",
        })

    dashboards = []
    for donor in donors:
        donor_donations = donations_by_donor[donor["_id"]]
        donor_donations.sort(key=lambda donation: donation["donatedAt"]["$date"], reverse=True)
        dashboards.append({
            "_id": donor["_id"],
            "_class": "xyz.nardone.aide.largescale.entity.DonorDashboardEntity",
            "donations": donor_donations,
            "suggestedCampaigns": [],
        })
    return dashboards


def update_organization_dashboards(dashboards, campaigns):
    """Copy final campaign raised amounts into organization dashboard summaries."""
    campaign_by_id = {campaign["_id"]["$oid"]: campaign for campaign in campaigns}
    for dashboard in dashboards:
        for summary in dashboard["openCampaigns"]:
            summary["raisedAmount"] = campaign_by_id[summary["campaignId"]]["raisedAmount"]


def simulate(donors, campaigns, total_donations, p, reward_probability, seed):
    """Generate donations and update campaign projections for all campaigns."""
    rng = random.Random(seed)
    counts = donor_donation_counts(donors, total_donations, p, rng)

    # Donation slots decide how frequently each donor appears in the simulation.
    slots = [donor for donor, count in zip(donors, counts) for _ in range(count)]
    rng.shuffle(slots)

    donor_totals = Counter(donor["_id"] for donor in slots)
    donations = []
    cursor = 0

    for campaign in campaigns:
        # Each campaign receives the same number of donations as its scraped backer count.
        rewards = campaign.get("availableRewards") or []
        count = int(campaign.get("donationsCount") or 0)
        donor_ids = slots[cursor : cursor + count]
        amounts = donation_amounts(campaign.get("raisedAmount"), count, rng)
        cursor += count
        campaign_donations = []

        for donor, amount_cents in zip(donor_ids, amounts):
            reward = rng.choice(rewards) if rewards and rng.random() < reward_probability else None
            status = rng.choice(["PENDING", "CONCLUDED"]) if reward else "CONCLUDED"
            donation = build_donation(
                len(donations) + 1,
                donor,
                campaign,
                amount_cents,
                reward,
                status,
                random_donation_date(campaign, rng),
            )
            donations.append(donation)
            campaign_donations.append(donation)

        # Campaigns embed the projections used by public and organization reads.
        update_campaign(campaign, campaign_donations)

    return donor_totals, donations


def write_chart(path, donors, donor_totals, p, total_donations):
    """Write a PNG chart showing the generated donor frequency distribution."""
    sorted_counts = sorted((donor_totals.get(donor["_id"], 0) for donor in donors), reverse=True)
    max_count = max(sorted_counts) if sorted_counts else 1

    path.parent.mkdir(parents=True, exist_ok=True)

    fig, ax = plt.subplots(figsize=(14, 8))
    ax.plot(range(1, len(sorted_counts) + 1), sorted_counts, color="#2563eb", linewidth=2.0)
    ax.set_title("Donor donation frequency simulation", fontsize=20, fontweight="bold", color="#12395b", pad=18)
    ax.set_xlabel("Donor rank", fontsize=12)
    ax.set_ylabel("Number of donations", fontsize=12)
    ax.set_xlim(1, max(1, len(sorted_counts)))
    ax.set_ylim(0, max_count + 1)
    ax.grid(axis="y", color="#e4e8ef", linewidth=1)
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)

    subtitle = (
        f"donors={len(donors)} | donation slots={total_donations} | geometric p={p:.6f}"
    )
    fig.text(0.125, 0.91, subtitle, fontsize=10.5, color="#506070")
    fig.tight_layout(rect=[0, 0, 1, 0.88])
    fig.savefig(path, dpi=160)
    plt.close(fig)
    return path


def main():
    """Run the donation simulation and write all derived Mongo JSON files."""
    parser = argparse.ArgumentParser(description="Simulate donation distribution and draw donor frequency chart.")
    parser.add_argument("--donors", type=Path, default=DONORS_FILE)
    parser.add_argument("--campaigns", type=Path, default=CAMPAIGNS_FILE)
    parser.add_argument("--organization-dashboards", type=Path, default=ORGANIZATION_DASHBOARDS_FILE)
    parser.add_argument("--donations-output", type=Path, default=DONATIONS_FILE)
    parser.add_argument("--donor-dashboards-output", type=Path, default=DONOR_DASHBOARDS_FILE)
    parser.add_argument("--chart-output", type=Path, default=CHART_FILE)
    parser.add_argument("--seed", type=int, default=SEED)
    parser.add_argument("--p", type=float, default=None)
    parser.add_argument("--reward-probability", type=float, default=REWARD_PROBABILITY)
    args = parser.parse_args()
    donors = json.loads(args.donors.read_text(encoding="utf-8"))
    campaigns = json.loads(args.campaigns.read_text(encoding="utf-8"))
    organization_dashboards = json.loads(args.organization_dashboards.read_text(encoding="utf-8"))
    corrected_campaigns = normalize_campaign_counts(campaigns)
    total_donations = sum(int(campaign.get("donationsCount") or 0) for campaign in campaigns)
    p = args.p or min(1.0, len(donors) / total_donations)

    # Generate donations first, then derive dashboard and campaign projections from them.
    donor_totals, donations = simulate(donors, campaigns, total_donations, p, args.reward_probability, args.seed)
    donor_dashboards = build_donor_dashboards(donors, donations)
    update_organization_dashboards(organization_dashboards, campaigns)

    # Rewrite the seed files that contain donation-dependent projections.
    args.donations_output.parent.mkdir(parents=True, exist_ok=True)
    args.donations_output.write_text(json.dumps(donations, indent=2, ensure_ascii=False), encoding="utf-8")
    args.campaigns.write_text(json.dumps(campaigns, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    args.organization_dashboards.write_text(json.dumps(organization_dashboards, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    args.donor_dashboards_output.write_text(json.dumps(donor_dashboards, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    chart_path = write_chart(
        args.chart_output,
        donors,
        donor_totals,
        p,
        total_donations,
    )

    print(f"donors: {len(donors)}")
    print(f"campaigns: {len(campaigns)}")
    print(f"campaigns normalized from zero to one donation: {corrected_campaigns}")
    print(f"donation slots: {total_donations}")
    print(f"simulated donations: {len(donations)}")
    print(f"donor dashboards: {len(donor_dashboards)}")
    print(f"geometric p: {p:.6f}")
    print(f"max donations by one donor: {max(donor_totals.values())}")
    print(f"donations file: {args.donations_output}")
    print(f"chart: {chart_path}")


if __name__ == "__main__":
    main()
