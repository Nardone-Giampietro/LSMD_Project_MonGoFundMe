#!/usr/bin/env python3
"""
Seed the local Neo4j graph from the generated Mongo JSON documents.
"""

import json
from pathlib import Path

from neo4j import GraphDatabase


ROOT = Path(__file__).resolve().parents[1]
ORGANIZATIONS_FILE = ROOT / "data/mongo/organization_users.json"
CAMPAIGNS_FILE = ROOT / "data/mongo/campaigns.json"
DONORS_FILE = ROOT / "data/mongo/donor_users.json"
DONATIONS_FILE = ROOT / "data/mongo/donations.json"

NEO4J_URI = "neo4j://127.0.0.1:7687"
NEO4J_USER = "neo4j"
NEO4J_PASSWORD = "qwe15001qwe"
NEO4J_DATABASE = "neo4j"
BATCH_SIZE = 5000


def load_json(path):
    """Read a JSON document list from disk."""
    return json.loads(path.read_text(encoding="utf-8"))


def batches(items):
    """Yield fixed-size batches for Cypher UNWIND statements."""
    for index in range(0, len(items), BATCH_SIZE):
        yield items[index:index + BATCH_SIZE]


def run_batches(session, query, rows):
    """Execute one Cypher query over all row batches."""
    for batch in batches(rows):
        session.run(query, rows=batch).consume()


def donation_relationships(donations):
    """Build one DONATED_BY relationship per campaign/donor pair."""
    latest = {}
    for donation in donations:
        key = (donation["campaignId"]["$oid"], donation["donorId"])
        donated_at = donation["donatedAt"]["$date"].replace("Z", "")
        latest[key] = max(latest.get(key, ""), donated_at)
    return [
        {"campaignId": campaign_id, "donorId": donor_id, "lastDonatedAt": last_donated_at}
        for (campaign_id, donor_id), last_donated_at in latest.items()
    ]


def main():
    """Recreate the local Neo4j graph from the generated Mongo JSON files."""
    # Convert Mongo-style documents into flat rows used by the Cypher imports.
    organizations = [{"id": item["_id"]} for item in load_json(ORGANIZATIONS_FILE)]
    donors = [{"id": item["_id"]} for item in load_json(DONORS_FILE)]
    campaigns = [
        {
            "id": item["_id"]["$oid"],
            "organizationId": item["organizationId"],
            "title": item["title"],
            "thumbnailImageUrl": item["thumbnailImageUrl"],
            "open": item.get("status") == "OPEN",
        }
        for item in load_json(CAMPAIGNS_FILE)
    ]
    donations = donation_relationships(load_json(DONATIONS_FILE))

    with GraphDatabase.driver(NEO4J_URI, auth=(NEO4J_USER, NEO4J_PASSWORD)) as driver:
        with driver.session(database=NEO4J_DATABASE) as session:
            # Similarity is rebuilt later by the application scheduler.
            session.run("CALL gds.graph.drop('campaign-similarity-graph', false)").consume()
            session.run("MATCH (n) DETACH DELETE n").consume()

            # Nodes are created before relationships so all endpoints exist.
            run_batches(session, "UNWIND $rows AS row MERGE (:Organization {id: row.id})", organizations)
            run_batches(session, "UNWIND $rows AS row MERGE (:Donor {id: row.id})", donors)
            run_batches(session, """
                UNWIND $rows AS row
                MERGE (organization:Organization {id: row.organizationId})
                MERGE (campaign:Campaign {id: row.id})
                SET campaign.title = row.title,
                    campaign.thumbnailImageUrl = row.thumbnailImageUrl,
                    campaign.open = row.open
                MERGE (organization)-[:CREATED]->(campaign)
            """, campaigns)
            # Donation relationships keep only the latest donation timestamp per pair.
            run_batches(session, """
                UNWIND $rows AS row
                MATCH (campaign:Campaign {id: row.campaignId})
                MATCH (donor:Donor {id: row.donorId})
                MERGE (campaign)-[donatedBy:DONATED_BY]->(donor)
                SET donatedBy.lastDonatedAt = localdatetime(row.lastDonatedAt)
            """, donations)

    print(json.dumps({
        "organizations": len(organizations),
        "donors": len(donors),
        "campaigns": len(campaigns),
        "donationRelationships": len(donations),
    }, indent=2))


if __name__ == "__main__":
    main()
