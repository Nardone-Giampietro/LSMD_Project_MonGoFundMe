#!/usr/bin/env python3
"""
Fetch random addresses and save them as text rows:

city,street,zip
"""

import argparse
import csv
import time
from pathlib import Path

import requests


ROOT = Path(__file__).resolve().parents[1]
API_URL = "https://randomuser.me/api/"
OUTPUT_FILE = ROOT / "data/raw/random_addresses.txt"
BATCH_SIZE = 5000
SLEEP_SECONDS = 0.5
NATIONALITIES = "au,br,ca,ch,de,dk,es,fi,fr,gb,ie,in,mx,nl,no,nz,rs,tr,ua,us"


def street_text(location):
    """Format the street number and name returned by randomuser.me."""
    street = location.get("street") or {}
    number = street.get("number")
    name = street.get("name") or ""
    return f"{number} {name}".strip()


def fetch_addresses(count, seed, nationalities):
    """Fetch complete address rows from randomuser.me until count is reached."""
    addresses = []
    page = 1

    while len(addresses) < count:
        # The seed and page keep repeated runs stable while still using API batches.
        response = requests.get(
            API_URL,
            params={
                "results": min(BATCH_SIZE, count - len(addresses)),
                "page": page,
                "seed": seed,
                "nat": nationalities,
                "inc": "location",
            },
            timeout=60,
        )
        data = response.json()

        # Keep only addresses that contain every field needed by Mongo seed users.
        for item in data.get("results", []):
            location = item.get("location") or {}
            city = str(location.get("city") or "").strip()
            street = street_text(location)
            zip_code = str(location.get("postcode") or "").strip()

            if city and street and zip_code:
                addresses.append((city, street, zip_code))

        print(f"addresses collected: {len(addresses)}")
        page += 1
        time.sleep(SLEEP_SECONDS)

    return addresses[:count]


def write_addresses(path, addresses):
    """Write address tuples as CSV rows without a header."""
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.writer(file)
        writer.writerows(addresses)


def build_parser():
    """Create the command-line parser for the address fetcher."""
    parser = argparse.ArgumentParser(description="Fetch random addresses from randomuser.me.")
    parser.add_argument("--count", type=int, default=10000)
    parser.add_argument("--output", type=Path, default=OUTPUT_FILE)
    parser.add_argument("--seed", default="mongofundme-addresses")
    parser.add_argument("--nationalities", default=NATIONALITIES)
    return parser


def main():
    """Fetch random addresses and save them in the raw data folder."""
    args = build_parser().parse_args()
    addresses = fetch_addresses(args.count, args.seed, args.nationalities)
    write_addresses(args.output, addresses)
    print(f"saved addresses: {len(addresses)}")
    print(f"output: {args.output}")


if __name__ == "__main__":
    main()
