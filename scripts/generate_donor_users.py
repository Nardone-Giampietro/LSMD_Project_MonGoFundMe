#!/usr/bin/env python3
"""
Generate synthetic donor user_account documents from randomuser.me.
"""

import argparse
import hashlib
import json
import time
from datetime import datetime, timezone
from pathlib import Path

import requests


ROOT = Path(__file__).resolve().parents[1]
API_URL = "https://randomuser.me/api/"
OUTPUT_FILE = ROOT / "data/mongo/donor_users.json"
DONOR_COUNT = 10000
BATCH_SIZE = 200
SLEEP_SECONDS = 0.5
REQUEST_TIMEOUT = (10, 20)
PASSWORD_HASH = "$2a$10$l/IzdOGtXPXrbvuXhqJhIONVxWmZesXEu/qcJuuwrqAHso1AFesXy"  # password123
NATIONALITIES = "au,br,ca,ch,de,dk,es,fi,fr,gb,ie,in,mx,nl,no,nz,rs,tr,ua,us"


def clean_text(value):
    """Normalize optional API values into stripped strings."""
    return "" if value is None else str(value).strip()


def unique_email(email, index, seen):
    """Return a normalized email only if it was not generated before."""
    email = clean_text(email).lower()
    if email and email not in seen:
        seen.add(email)
        return email
    return None


def donor_document(item, index, seen_emails):
    """Convert one randomuser.me item into a donor user_account document."""
    name = item.get("name") or {}
    location = item.get("location") or {}
    street = location.get("street") or {}
    first_name = clean_text(name.get("first"))
    last_name = clean_text(name.get("last"))
    email = unique_email(item.get("email"), index, seen_emails)
    if not email:
        return None
    now = {"$date": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")}

    return {
        "_id": "donor_" + hashlib.sha1(email.encode("utf-8")).hexdigest()[:16],
        "_class": "xyz.nardone.aide.largescale.entity.DonorEntity",
        "displayName": f"{first_name} {last_name}".strip(),
        "email": email,
        "password": PASSWORD_HASH,
        "createdAt": now,
        "updatedAt": now,
        "role": {"name": "ROLE_DONOR"},
        "location": {
            "city": clean_text(location.get("city")),
            "street": f"{street.get('number')} {street.get('name') or ''}".strip(),
            "zip": clean_text(location.get("postcode")),
        },
        "firstName": first_name,
        "lastName": last_name,
    }


def fetch_batch(page, count):
    """Request one page of donor data with basic retry handling."""
    for _ in range(3):
        try:
            response = requests.get(
                API_URL,
                params={
                    "results": count,
                    "page": page,
                    "nat": NATIONALITIES,
                    "inc": "name,email,location",
                },
                timeout=REQUEST_TIMEOUT,
            )
            if response.status_code == 200:
                return response.json().get("results", [])
        except Exception:
            pass
        time.sleep(SLEEP_SECONDS)
    return []


def fetch_donors(count):
    """Generate donor documents until the requested count is reached."""
    donors = []
    seen_emails = set()
    page = 1
    failed_pages = 0

    while len(donors) < count:
        # Request only the number of users still needed for the output file.
        items = fetch_batch(page, min(BATCH_SIZE, count - len(donors)))
        if not items:
            failed_pages += 1
            if failed_pages >= 2:
                raise RuntimeError("randomuser.me did not return enough users")
            page += 1
            continue
        failed_pages = 0

        # Duplicate emails are skipped because user_account.email is unique.
        for item in items:
            donor = donor_document(item, len(donors) + 1, seen_emails)
            if donor:
                donors.append(donor)

        print(f"donors generated: {len(donors)}")
        page += 1
        time.sleep(SLEEP_SECONDS)

    return donors[:count]


def main():
    """Generate donor user_account JSON documents from randomuser.me data."""
    parser = argparse.ArgumentParser(description="Generate donor user_account MongoDB documents.")
    parser.add_argument("--count", type=int, default=DONOR_COUNT)
    parser.add_argument("--output", type=Path, default=OUTPUT_FILE)
    args = parser.parse_args()

    donors = fetch_donors(args.count)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(donors, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"output": str(args.output), "donors": len(donors)}, indent=2))


if __name__ == "__main__":
    main()
