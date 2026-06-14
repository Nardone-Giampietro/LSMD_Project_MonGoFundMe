#!/usr/bin/env python3
"""
Scrapy-based Indiegogo crawler.

This writes raw campaign documents with Scrapy as the crawler framework and
scrapy-playwright for rendered pages.

Example:
  python3 scripts/indiegogo_scrapy_campaigns.py --max-campaigns 5
"""

import argparse
import asyncio
import json
import random
import time
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urljoin, urlparse

import jmespath
import requests
import scrapy
from bs4 import BeautifulSoup
from parsel import Selector
from scrapy.crawler import CrawlerProcess


ROOT = Path(__file__).resolve().parents[1]
API_PROJECT_URL = "https://www.indiegogo.com/api/public/projects/getCrowdfundingProject"
SEARCH_API_URL = "https://www.indiegogo.com/api/projectSearch/searchProjectsForCards"
OUTPUT_PATH = ROOT / "data/raw/indiegogo_campaigns_raw.json"
MAX_UPDATES = 100
TIMEOUT_SECONDS = 60
API_TIMEOUT_SECONDS = 30
PAGE_MARKER_WAIT_MS = 8000
UPDATE_LIST_WAIT_MS = 5000
SEARCH_WAIT_MS = 5000
REQUEST_RETRIES = 2
CAMPAIGN_CONCURRENCY = 2
UPDATE_CONCURRENCY = 4
SLEEP_SECONDS = 0.5
BLOCKED_RESOURCE_TYPES = {"image", "media", "font", "stylesheet", "manifest"}
USER_AGENT = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/125.0.0.0 Safari/537.36"
)
API_HEADERS = {"Accept": "application/json", "Content-Type": "application/json", "User-Agent": USER_AGENT}
JSON_DECODER = json.JSONDecoder()


def should_abort_request(request):
    """Return true for browser resources that are not needed for scraping data."""
    # The campaign data is embedded in scripts, so images/fonts/media are not needed.
    return request.resource_type in BLOCKED_RESOURCE_TYPES


def write_json(path, data):
    """Write pretty JSON and create the parent directory when needed."""
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def html_to_text(html):
    """Convert an HTML fragment into compact plain text."""
    if not html:
        return ""
    text = BeautifulSoup(html, "lxml").get_text("\n", strip=True)
    return "\n".join(line.strip() for line in text.splitlines() if line.strip())


def pick(data, path, default=None):
    """Read a nested value with JMESPath and return a default when missing."""
    value = jmespath.search(path, data)
    return default if value is None else value


def json_at(text, marker, keep_marker=False):
    """Decode the first JSON object found after a text marker."""
    start = text.find(marker)
    if start == -1:
        return {}
    start = start if keep_marker else start + len(marker)
    try:
        return JSON_DECODER.raw_decode(text[start:].lstrip(" =\n\t"))[0]
    except ValueError:
        return {}


def initial_state(html):
    """Extract Indiegogo's window.__INITIAL_STATE__ object from a page."""
    return json_at(html, "window.__INITIAL_STATE__")


def component_blocks(html, component_name):
    """Extract React component JSON blocks matching a component name."""
    return [
        json_at(script, '{"props":', keep_marker=True)
        for script in Selector(text=html).css("script::text").getall()
        if component_name in script and '{"props":' in script
    ]


def component_data(html, component_name):
    """Return the first matching React component JSON block."""
    return next(iter(component_blocks(html, component_name)), {})


def search_api_payload(page_index):
    """Build the search API request body for one result page."""
    return {
        "creatorID": None,
        "creatorName": None,
        "sortType": 0,
        "term": "",
        "projectPhaseSearchTypes": [0, 4],
        "projectBenefits": [],
        "projectTags": [],
        "projectCatalogCategories": [],
        "playerAges": [],
        "playerCounts": [],
        "playTimes": [],
        "creator": {"creatorID": None, "name": None},
        "userCommunitySearchTypes": [],
        "source": 5,
        "pageIndex": page_index,
    }


def card_project_url(project):
    """Return the canonical Indiegogo project URL from one search card."""
    # The search API can also return Gamefound projects. Keep only real Indiegogo URLs.
    url = pick(project, "url")
    if url and urlparse(url).netloc.replace(":443", "") == "www.indiegogo.com":
        return url.replace(":443", "")

    if pick(project, "platform") != 1:
        return None

    creator_url = pick(project, "creator.urlName")
    project_url = pick(project, "projectUrlName")
    return f"https://www.indiegogo.com/en/projects/{creator_url}/{project_url}" if creator_url and project_url else None


def get_search_projects(page_index):
    """Fetch one page of campaign cards from the Indiegogo search API."""
    return request_json("post", SEARCH_API_URL, json=search_api_payload(page_index)).get("projects", {})


def request_json(method, url, **kwargs):
    """Call a JSON API endpoint with retries and return an empty dict on failure."""
    for _ in range(REQUEST_RETRIES + 1):
        try:
            response = requests.request(method, url, headers=API_HEADERS, timeout=API_TIMEOUT_SECONDS, **kwargs)
            if response.status_code == 200:
                return response.json()
        except Exception:
            pass
        time.sleep(SLEEP_SECONDS)

    return {}


def project_url_name(url):
    """Extract the project urlName segment from an Indiegogo URL."""
    return urlparse(url).path.rstrip("/").split("/")[-1]


def creator_url_name(url):
    """Extract the creator urlName segment from an Indiegogo URL."""
    return urlparse(url).path.rstrip("/").split("/")[-2]


def get_api_project(url_name):
    """Fetch the public project API details for a campaign urlName."""
    return request_json("get", API_PROJECT_URL, params={"urlName": url_name})


def random_tags(home_html, rng):
    """Select up to three project tags from the campaign story component."""
    story = component_data(home_html, "App.Components.Projects.ProjectSectionStory")
    tags = pick(story, "props.projectTags[].name", [])
    return rng.sample(tags, min(3, len(tags)))


def full_description(home_html):
    """Build the long campaign description from story and content sections."""
    # Indiegogo stores the long story in React component script blocks.
    excluded_headers = {
        "Rewards",
        "Add-ons",
        "Risks and challenges",
        "Refunds and cancellation",
        "Shipping and Taxes",
        "FAQ",
        "Legal & Compliance",
    }
    parts = []

    story = component_data(home_html, "App.Components.Projects.ProjectSectionStory")
    story_text = html_to_text(pick(story, "props.content", ""))
    if story_text:
        parts.append(story_text)

    for section in component_blocks(home_html, "App.Components.Projects.ProjectSectionContent"):
        header = pick(section, "props.header") or pick(section, "props.originalHeader", "")
        content = html_to_text(pick(section, "props.content", ""))
        if content and header not in excluded_headers:
            parts.append(f"{header}\n{content}".strip())

    return "\n\n".join(parts)


def rewards(home_html):
    """Extract available campaign rewards from the rewards component."""
    data = component_data(home_html, "App.Components.Projects.ProjectSectionRewards")
    result = []

    for reward in pick(data, "props.rewards", []):
        description = pick(reward, "abstract", "")
        remarks = html_to_text(pick(reward, "deliveryDateRemarks", ""))
        if remarks and remarks not in description:
            description = (description + "\n" + remarks).strip()

        result.append(
            {
                "title": pick(reward, "name", ""),
                "description": description,
                "amount": pick(reward, "effectivePrice") or pick(reward, "price", 0),
            }
        )

    return result


def milestones(state):
    """Convert Indiegogo stretch goals into campaign milestones."""
    # Stretch goals are used as campaign milestones in our raw document.
    return [
        {
            "title": pick(goal, "name", ""),
            "targetAmount": pick(goal, "targetValue", 0),
            "status": "VERIFIED" if pick(goal, "isUnlocked") else "PENDING",
            "verificationDate": pick(goal, "unlockedAt"),
        }
        for goal in pick(state, "projectState.stretchGoals", [])
        if pick(goal, "isPublished") is not False
    ]


def update_from_page(update_html):
    """Extract one update summary from an update detail page."""
    data = component_data(update_html, "App.Components.Projects.ProjectUpdate")
    update = pick(data, "props.update", {})

    return {
        "date": pick(update, "publishedAt") or pick(update, "createdAt", ""),
        "title": pick(update, "title", ""),
        "description": html_to_text(pick(update, "content") or pick(update, "abstract", "")),
    }


def parse_date(value):
    """Parse a timestamp into a timezone-aware UTC datetime."""
    try:
        return datetime.fromisoformat(str(value).replace("Z", "+00:00")).astimezone(timezone.utc) if value else None
    except ValueError:
        return None


def status(end_date, goal_amount, raised_amount, funded_at):
    """Infer the application campaign status from funding and end-date data."""
    if funded_at or (goal_amount and raised_amount >= goal_amount):
        return "CONCLUDED"
    if end_date and end_date <= datetime.now(timezone.utc):
        return "CLOSED"
    return "OPEN"


def source_values(state, api_data):
    """Collect the main project, creator, statistics, and API payload objects."""
    project = pick(state, "projectContext.project", {})
    return project, pick(state, "projectContext.creator") or pick(project, "creator", {}), pick(state, "projectState.statistics", {}), api_data or {}


def thumbnail_image_url(api, project):
    """Resolve the best available campaign thumbnail image URL."""
    image_url = pick(api, "projectImageUrl") or pick(project, "tileImageUrl")
    image_file = pick(project, "tileImageFileName")
    project_id = pick(project, "projectID")
    if image_url or not (image_file and project_id):
        return image_url
    return f"https://cdn.images.indiegogo.com/projectimage/projects/{project_id}/{image_file}"


async def rendered_html(page, url=None):
    """Load a page and return HTML after project data is available."""
    for _ in range(REQUEST_RETRIES):
        if url:
            await page.goto(url, wait_until="domcontentloaded", timeout=TIMEOUT_SECONDS * 1000)

        try:
            # Wait for useful project data instead of sleeping a fixed amount on every page.
            await page.wait_for_function(
                """
                () => {
                  const title = document.title.toLowerCase();
                  const body = (document.body && document.body.innerText || "").toLowerCase();
                  if (title.includes("just a moment") || body.includes("security verification")) return false;
                  if (window.__INITIAL_STATE__) return true;
                  return Array.from(document.scripts).some(script => {
                    const text = script.textContent || "";
                    return text.includes("App.Components.Projects") || text.includes('{"props":');
                  });
                }
                """,
                timeout=PAGE_MARKER_WAIT_MS,
            )
        except Exception:
            pass

        body = await page.locator("body").inner_text(timeout=10000)
        title = await page.title()
        if "security verification" not in body.lower() and "just a moment" not in title.lower():
            return await page.content()

        await page.wait_for_timeout(8000)

    return await page.content()


async def click_load_more(page):
    """Click the updates LOAD MORE button when more update cards are available."""
    # The updates page loads older entries behind a disabled/enabled LOAD MORE button.
    buttons = page.locator("button:has-text('LOAD MORE')")
    if await buttons.count() == 0:
        return False

    for _ in range(6):
        button = buttons.last
        try:
            link_count = await page.locator('a[data-qa^="update-container:"]').count()
            await button.scroll_into_view_if_needed(timeout=5000)
            if await button.is_enabled(timeout=2000):
                await button.click(timeout=10000)
                try:
                    await page.wait_for_function(
                        """
                        ({count}) => document.querySelectorAll('a[data-qa^="update-container:"]').length > count
                        """,
                        {"count": link_count},
                        timeout=SEARCH_WAIT_MS,
                    )
                except Exception:
                    await page.wait_for_timeout(1000)
                return True
        except Exception:
            pass
        await page.wait_for_timeout(1000)

    return False


async def collect_update_links(page, updates_url):
    """Collect update detail links from the campaign updates page."""
    await rendered_html(page, updates_url)
    try:
        # The page can be technically loaded before the update cards are mounted.
        await page.wait_for_selector(
            'a[data-qa^="update-container:"], button:has-text("LOAD MORE")',
            timeout=UPDATE_LIST_WAIT_MS,
        )
    except Exception:
        pass

    links = []
    seen = set()

    for _ in range(10):
        hrefs = await page.locator('a[data-qa^="update-container:"]').evaluate_all(
            "els => els.map(a => a.href)"
        )
        for href in hrefs:
            if href not in seen:
                seen.add(href)
                links.append(href)
                if len(links) >= MAX_UPDATES:
                    break

        if len(links) >= MAX_UPDATES:
            break

        if not await click_load_more(page):
            break

    return links


async def collect_updates(page, campaign_url):
    """Fetch and parse all collected update detail pages for one campaign."""
    result = []
    links = await collect_update_links(page, campaign_url.rstrip("/") + "/updates")

    async def fetch_update(link):
        """Fetch one update page using an isolated Playwright page."""
        # Update detail pages are independent, so a few browser pages can run in parallel.
        update_page = await page.context.new_page()
        try:
            return update_from_page(await rendered_html(update_page, link))
        finally:
            await update_page.close()

    semaphore = asyncio.Semaphore(UPDATE_CONCURRENCY)

    async def limited_fetch(link):
        """Limit update detail requests to the configured concurrency."""
        async with semaphore:
            return await fetch_update(link)

    result = await asyncio.gather(*(limited_fetch(link) for link in links))

    return result


class IndiegogoSpider(scrapy.Spider):
    """Scrapy spider that collects rendered Indiegogo campaign documents."""

    name = "indiegogo_scrapy_campaigns"
    custom_settings = {
        "TWISTED_REACTOR": "twisted.internet.asyncioreactor.AsyncioSelectorReactor",
        "DOWNLOAD_HANDLERS": {
            "http": "scrapy_playwright.handler.ScrapyPlaywrightDownloadHandler",
            "https": "scrapy_playwright.handler.ScrapyPlaywrightDownloadHandler",
        },
        "PLAYWRIGHT_BROWSER_TYPE": "chromium",
        # Abort heavy browser assets to keep the crawler fast without losing embedded data.
        "PLAYWRIGHT_ABORT_REQUEST": should_abort_request,
        "PLAYWRIGHT_LAUNCH_OPTIONS": {
            "headless": True,
            "args": ["--disable-blink-features=AutomationControlled"],
        },
        "PLAYWRIGHT_CONTEXTS": {
            "default": {
                "user_agent": USER_AGENT,
                "viewport": {"width": 1365, "height": 900},
                "locale": "en-US",
            }
        },
        "PLAYWRIGHT_MAX_PAGES_PER_CONTEXT": 10,
        "CONCURRENT_REQUESTS": CAMPAIGN_CONCURRENCY,
        "DOWNLOAD_TIMEOUT": TIMEOUT_SECONDS,
        "LOG_LEVEL": "INFO",
        "ROBOTSTXT_OBEY": False,
        "USER_AGENT": USER_AGENT,
    }

    def __init__(self, max_campaigns=2000, output=OUTPUT_PATH, *args, **kwargs):
        """Initialize crawl limits, output path, and in-memory crawl state."""
        super().__init__(*args, **kwargs)
        self.max_campaigns = int(max_campaigns)
        self.output = Path(output)
        self.documents = []
        self.pending_links = []
        self.seen_links = set()
        self.page_index = 0
        self.attempts = 0
        self.active_requests = 0
        self.no_more_links = False
        self.rng = random.Random()

    async def start(self):
        """Start the crawl by scheduling the initial campaign requests."""
        # Keep only a few pages in flight and request more URLs only when needed.
        print(f"scraping campaigns: {self.max_campaigns}")
        for _ in range(CAMPAIGN_CONCURRENCY):
            request = self.next_campaign_request()
            if request:
                yield request

    def next_campaign_url(self):
        """Return the next unseen campaign URL, fetching search pages as needed."""
        # Search result pages are cheap to read through the API; campaign details need the browser later.
        while not self.pending_links and not self.no_more_links:
            items = pick(get_search_projects(self.page_index), "pagedItems", [])
            if not items:
                self.no_more_links = True
                break

            for item in items:
                url = card_project_url(item)
                if url and url not in self.seen_links:
                    self.seen_links.add(url)
                    self.pending_links.append(url)

            self.page_index += 1
            print(f"collected candidate links: {len(self.seen_links)}")
            time.sleep(SLEEP_SECONDS)

        return self.pending_links.pop(0) if self.pending_links else None

    def next_campaign_request(self):
        """Build the next Scrapy request while respecting the campaign limit."""
        if len(self.documents) + self.active_requests >= self.max_campaigns:
            return None

        url = self.next_campaign_url()
        if not url:
            return None

        self.attempts += 1
        self.active_requests += 1
        return scrapy.Request(
            url,
            callback=self.parse_campaign,
            errback=self.parse_error,
            cb_kwargs={"index": self.attempts},
            meta={
                "playwright": True,
                "playwright_include_page": True,
                "campaign_index": self.attempts,
                "handle_httpstatus_all": True,
            },
            dont_filter=True,
        )

    async def parse_campaign(self, response, index):
        """Parse one rendered campaign response and schedule the next request."""
        page = response.meta["playwright_page"]
        url = response.url

        try:
            print(f"attempt {index} - saved {len(self.documents)}/{self.max_campaigns} {url}")
            if response.status >= 400:
                raise RuntimeError(f"http status {response.status}")
            document = await self.scrape_campaign(page, url)
            if len(self.documents) < self.max_campaigns:
                self.documents.append((len(self.documents) + 1, document))
                print(
                    "  "
                    f"saved={len(self.documents)}/{self.max_campaigns} "
                    f"status={document['status']} "
                    f"rewards={len(document['availableRewards'])} "
                    f"updates={len(document['updates'])} "
                    f"milestones={len(document['milestones'])}"
                )
        except Exception as exc:
            print(f"  error, not counted: {exc}")
        finally:
            await page.close()
            self.active_requests = max(0, self.active_requests - 1)

        request = self.next_campaign_request()
        return [request] if request else []

    async def scrape_campaign(self, page, url):
        """Scrape all raw fields needed to later build MongoDB campaign documents."""
        # Merge webpage data with the public API fields that are still useful.
        url_name = project_url_name(url)
        api_data = get_api_project(url_name)
        home_html = await rendered_html(page)
        state = initial_state(home_html)
        project, creator, stats, api = source_values(state, api_data)

        goal_amount = pick(api, "campaignGoal") or pick(project, "campaignGoal", 0)
        raised_amount = pick(api, "fundsGathered") or pick(stats, "totalFundsGathered") or pick(stats, "fundsGathered", 0)
        start_date = pick(api, "campaignStartDate") or pick(project, "campaignStart")
        end_date = pick(api, "campaignEndDate") or pick(project, "campaignEnd")
        backer_count = pick(api, "backerCount") or pick(stats, "totalBackersCount") or pick(stats, "backersCount", 0)
        campaign_updates = await collect_updates(page, url)
        campaign_rewards = rewards(home_html)
        campaign_milestones = milestones(state)

        creator_name = pick(api, "creatorName") or pick(creator, "name") or pick(project, "creatorName", "")
        creator_url = pick(api, "creatorUrlName") or pick(creator, "urlName") or creator_url_name(url)

        description = full_description(home_html) or pick(api, "shortDescription") or pick(project, "shortDescription", "")

        return {
            "source": {
                "platform": "indiegogo",
                "projectId": pick(project, "projectID") or pick(state, "projectContext.projectID"),
                "projectUrl": url,
                "projectUrlName": url_name,
                "creatorId": pick(creator, "creatorID") or pick(project, "creatorID"),
                "creatorUrlName": creator_url,
            },
            "organizationSnapshot": {
                "name": creator_name,
                "city": pick(creator, "displayedLocation", ""),
            },
            "organization": {
                "sourceCreatorId": pick(creator, "creatorID") or pick(project, "creatorID"),
                "creatorUrlName": creator_url,
                "displayName": creator_name,
                "legalName": pick(creator, "creatorLegalEntityInfo.legalEntityName") or creator_name,
                "website": urljoin("https://www.indiegogo.com", pick(creator, "creatorPageUrl") or f"/creators/{creator_url}"),
                "description": pick(creator, "description", ""),
                "thumbImageUrl": pick(creator, "thumbImageUrl"),
            },
            "title": pick(api, "projectName") or pick(project, "name", ""),
            "thumbnailImageUrl": thumbnail_image_url(api, project),
            "description": description,
            "tags": random_tags(home_html, self.rng),
            "startDate": start_date,
            "endDate": end_date,
            "status": status(parse_date(end_date), float(goal_amount or 0), float(raised_amount or 0), pick(stats, "fundedAt")),
            "currency": "EUR",
            "goalAmount": goal_amount,
            "raisedAmount": raised_amount,
            "stats": {
                "donationsCount": backer_count,
                "pendingRewardsCount": 0,
                "milestonesTotalCount": len(campaign_milestones),
                "verifiedMilestonesCount": len([m for m in campaign_milestones if m["status"] == "VERIFIED"]),
            },
            "latestDonations": [],
            "milestones": campaign_milestones,
            "updates": campaign_updates,
            "availableRewards": campaign_rewards,
            "createdAt": start_date,
            "updatedAt": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        }

    async def parse_error(self, failure):
        """Close failed Playwright pages and keep the crawl moving."""
        page = failure.request.meta.get("playwright_page")
        if page:
            await page.close()
        self.active_requests = max(0, self.active_requests - 1)
        print(f"  request error, not counted: {failure.request.url} - {failure.value}")

        request = self.next_campaign_request()
        return [request] if request else []

    def closed(self, reason):
        """Write all collected documents when the Scrapy spider stops."""
        documents = [document for _, document in sorted(self.documents, key=lambda item: item[0])]
        write_json(self.output, documents)
        print(json.dumps({"output": str(self.output), "campaigns": len(documents), "reason": reason}, indent=2))


def build_parser():
    """Create the command-line parser for the Indiegogo crawler."""
    parser = argparse.ArgumentParser(description="Scrape raw Indiegogo campaign documents with Scrapy.")
    parser.add_argument("--max-campaigns", type=int, default=2000)
    parser.add_argument("--output", type=Path, default=OUTPUT_PATH)
    return parser


def main():
    """Run the Scrapy crawler with the configured command-line arguments."""
    args = build_parser().parse_args()
    process = CrawlerProcess()
    process.crawl(IndiegogoSpider, max_campaigns=args.max_campaigns, output=args.output)
    process.start()


if __name__ == "__main__":
    main()
