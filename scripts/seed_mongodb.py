#!/usr/bin/env python3
"""
Seed local MongoDB from the final generated JSON files.
"""

from pathlib import Path

from bson import json_util
from pymongo import MongoClient


ROOT = Path(__file__).resolve().parents[1]
MONGO_URI = (
    "mongodb://localhost:27017,localhost:27018,localhost:27019/mongofundme"
    "?replicaSet=rs0&readPreference=primary&readConcernLevel=majority&w=majority&wtimeoutMS=5000"
)
DATABASE = "mongofundme"
BATCH_SIZE = 5000

FILES = [
    ("user_account", ROOT / "data/mongo/organization_users.json", True),
    ("user_account", ROOT / "data/mongo/donor_users.json", False),
    ("campaign", ROOT / "data/mongo/campaigns.json", True),
    ("donation", ROOT / "data/mongo/donations.json", True),
    ("dashboard", ROOT / "data/mongo/organization_dashboards.json", True),
    ("dashboard", ROOT / "data/mongo/donor_dashboards.json", False),
]

AUXILIARY_COLLECTIONS = [
    "homepage_recommendation",
    "organization_pending_reward_aging",
    "organization_performance_ranking",
    "outbox_event",
]


def load_json(path):
    """Read a Mongo extended JSON document list from disk."""
    return json_util.loads(path.read_text(encoding="utf-8"))


def batches(items):
    """Yield fixed-size batches for bulk MongoDB inserts."""
    for index in range(0, len(items), BATCH_SIZE):
        yield items[index:index + BATCH_SIZE]


def insert_file(db, collection_name, path, drop):
    """Optionally reset one collection and insert the documents from one file."""
    if drop:
        db[collection_name].drop()

    documents = load_json(path)
    for batch in batches(documents):
        db[collection_name].insert_many(batch)

    print(f"{collection_name}: inserted {len(documents)} from {path}")


def main():
    """Seed the local MongoDB database with the generated project dataset."""
    with MongoClient(MONGO_URI) as client:
        db = client[DATABASE]

        # Derived collections are rebuilt by the application after the seed import.
        for collection_name in AUXILIARY_COLLECTIONS:
            db[collection_name].drop()

        # Load the generated source collections in the order expected by the app.
        for collection_name, path, drop in FILES:
            insert_file(db, collection_name, path, drop)

        # Print the final counts so the operator can verify the import quickly.
        print({
            "user_account": db.user_account.count_documents({}),
            "campaign": db.campaign.count_documents({}),
            "donation": db.donation.count_documents({}),
            "dashboard": db.dashboard.count_documents({}),
        })


if __name__ == "__main__":
    main()
