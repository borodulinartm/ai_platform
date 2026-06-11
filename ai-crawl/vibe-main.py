import asyncio
import json
import argparse
import logging
from collections import defaultdict
from datetime import datetime, date, timedelta
import asyncpg
from pydantic import BaseModel, Field
from crawl4ai import AsyncWebCrawler, CrawlerRunConfig, LLMConfig, LLMExtractionStrategy, BrowserConfig
from crawl4ai.async_configs import ProxyConfig
import litellm
import hashlib
import sys
import os

logging.basicConfig(
    level=logging.DEBUG if os.environ.get("DEBUG") else logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger("crawl")

os.environ['PYTHONIOENCODING'] = 'utf-8'
os.environ['LITELLM_LOG'] = 'WARNING'
sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')

litellm.suppress_debug_info = True
litellm.set_verbose = False
if not os.environ.get("DEBUG"):
    for name in ["litellm", "LiteLLM", "litellm.llm_api_call", "litellm.utils"]:
        logging.getLogger(name).setLevel(logging.WARNING)


def generate_article_hash(feed_id: int, title: str, published_date: str, category_name: str) -> bytes:
    normalized = title.replace('\u2018', "'").replace('\u2019', "'").replace('\u201c', '"').replace('\u201d', '"')
    content = f"{feed_id}:{normalized}:{published_date}:{category_name}"
    return hashlib.md5(content.encode()).digest()

DB_CONFIG = {
    "user": os.environ.get("DB_USER") or "freshrss",
    "password": os.environ.get("DB_PASSWORD") or "freshrss",
    "database": os.environ.get("DB_NAME") or "freshrss",
    "host": os.environ.get("DB_HOST") or "183.87.45.129",
    "port": int(os.environ.get("DB_PORT") or "5432")
}

class CategoryVerdict(BaseModel):
    category_name: str = Field(description="The EXACT name of the category being evaluated from the provided list.")
    is_relevant: bool = Field(description="TRUE if the article strictly matches this specific category's description. Otherwise FALSE.")

class MultiFilteredArticle(BaseModel):
    title: str = Field(description="Title of the article. Leave empty only if ALL categories are irrelevant.")
    author: str = Field(description="Author name, leave empty if not found.")
    content: str = Field(description="Full text content of the article clean of HTML. Leave empty only if ALL categories are irrelevant.")
    published_date: str = Field(description="Publication date of the article in YYYY-MM-DD format.")
    verdicts: list[CategoryVerdict] = Field(description="A list of verdicts for EVERY category requested in the prompt.")


async def main(log_only=False):
    conn = await asyncpg.connect(**DB_CONFIG)
    
    query = """
        SELECT f.id AS feed_id, f.url AS feed_url, c.name AS cat_name, c.description_en AS cat_desc
        FROM public.admin_feed f
        JOIN public.admin_category c ON f.category = c.id
        WHERE f.url LIKE 'scraped::%' AND c.name NOT ILIKE '%Uncategorized%';
    """
    rows = await conn.fetch(query)
    
    if not rows:
        log.warning("No matching 'scraped::' sources found in admin_feed")
        await conn.close()
        return

    url_groups = defaultdict(list)
    for row in rows:
        original_url = row['feed_url'].replace("scraped::", "", 1)
        url_groups[original_url].append({
            "feed_id": row['feed_id'],
            "cat_name": row['cat_name'],
            "cat_desc": row['cat_desc'] if row['cat_desc'] else "No description provided."
        })
        
    mode_str = "[LOG-ONLY]" if log_only else "[SAVE-TO-DB]"
    log.info("%s DB records: %d. Unique URLs to check: %d", mode_str, len(rows), len(url_groups))
    log.info("LLM config: provider=%s model=%s url=%s key=%s...%s",
             os.environ.get("LLM_PROVIDER") or "deepseek",
             os.environ.get("LLM_MODEL") or "deepseek-v4-flash",
             os.environ.get("LLM_URL") or "https://openrouter.ai/api/v1",
             (os.environ.get("LLM_KEY") or "")[:4],
             (os.environ.get("LLM_KEY") or "")[-4:])

    yesterday = date.today() - timedelta(days=1)
    log.info("Today: %s, filtering for yesterday: %s", date.today(), yesterday)
    source_counts = defaultdict(int)

    try:
        browser_config = BrowserConfig(
            headless=True,
            verbose=False,
        )
        async with AsyncWebCrawler(config=browser_config) as crawler:
            for original_url, target_feeds in url_groups.items():
                log.info("=" * 60)
                log.info("SCANNING: %s", original_url)
                log.info("Checking categories: %s", [f['cat_name'] for f in target_feeds])
                
                categories_block = ""
                for f in target_feeds:
                    categories_block += f"- Name: \"{f['cat_name']}\"\n  Criteria: {f['cat_desc']}\n\n"

                system_instruction = f"""
                You are a strict multi-category filtering assistant. Your task is to evaluate ALL articles on the webpage content against multiple requested categories at once.
                Extract EVERY article you can find on the page, ordered from newest to oldest.

                REQUESTED CATEGORIES TO EVALUATE:
                {categories_block}

                CRITICAL RULES:
                1. You MUST evaluate and provide a verdict (TRUE/FALSE) for EVERY category listed above. Do not skip any.
                2. Set "is_relevant" to TRUE only if the content perfectly matches that category's specific criteria.
                3. Special Rule for Russia: 'Company Insight | 公司洞察' and 'University Insight | 高校洞察' are STRICTLY EXCLUSIVE to Russian entities. If the text is about foreign companies (e.g. Apple) or foreign universities (e.g. MIT), you MUST set "is_relevant" to FALSE for them.
                4. If AT LEAST ONE category is TRUE, extract the full article title, author, and clean markdown content.
                5. If ALL categories are FALSE, you can leave title and content empty.
                6. You MUST extract ALL articles on the page, ordered from newest to oldest.
                """
                llm_provider = os.environ.get("LLM_PROVIDER") or "deepseek"
                llm_model = os.environ.get("LLM_MODEL") or "deepseek-v4-flash"
                llm_base_url = os.environ.get("LLM_URL") or "https://openrouter.ai/api/v1"
                if not llm_base_url.endswith("/v1"):
                    llm_base_url = llm_base_url.rstrip("/") + "/v1"
                llm_config = LLMConfig(
                    provider=f"openrouter/{llm_provider}/{llm_model}",
                    api_token=os.environ.get("LLM_KEY") or "",
                    base_url=llm_base_url
                )
                llm_strategy = LLMExtractionStrategy(
                    llm_config=llm_config,
                    schema=MultiFilteredArticle.model_json_schema(),
                    instruction=system_instruction,
                    input_format="markdown",
                    extra_args={
                        "temperature": 0.1
                    }
                )

                load_more_js = """
                    (async () => {
                        const loadMoreKeywords = ['load more', 'show more', 'показать ещё', 'показать еще', 'показать больше', 'загрузить ещё', 'загрузить еще', 'next', 'следующая', 'read more'];
                        
                        function findLoadMoreButton() {
                            const cssSelectors = [
                                '.news-list__more', '.load-more', '.load-more-btn', '.show-more',
                                '.next-page', '.pagination-next', '.btn-load-more', '[data-load-more]',
                                '.btn-show-more', '.more-btn', '.loadmore', '.show-more-btn'
                            ];
                            for (const sel of cssSelectors) {
                                const el = document.querySelector(sel);
                                if (el && el.offsetParent !== null) return el;
                            }
                            
                            for (const el of document.querySelectorAll('button, [role="button"]')) {
                                const aria = (el.getAttribute('aria-label') || '').toLowerCase();
                                if (loadMoreKeywords.some(kw => aria.includes(kw)) && el.offsetParent !== null) return el;
                            }
                            
                            for (const el of document.querySelectorAll('button, [role="button"], span, div')) {
                                if (el.children.length > 0) continue;
                                const text = el.textContent.trim().toLowerCase();
                                if (text.length > 3 && text.length < 50 && loadMoreKeywords.some(kw => text.includes(kw))) {
                                    if (el.offsetParent !== null) return el;
                                }
                            }
                            
                            return null;
                        }
                        
                        let prevHeight = 0;
                        let sameHeightCount = 0;
                        
                        for (let i = 0; i < 20; i++) {
                            window.scrollTo(0, document.body.scrollHeight);
                            await new Promise(r => setTimeout(r, 2000));
                            
                            const btn = findLoadMoreButton();
                            if (btn) {
                                btn.click();
                                await new Promise(r => setTimeout(r, 3000));
                            } else {
                                const currHeight = document.body.scrollHeight;
                                if (currHeight === prevHeight) {
                                    sameHeightCount++;
                                    if (sameHeightCount >= 2) break;
                                } else {
                                    sameHeightCount = 0;
                                    prevHeight = currHeight;
                                }
                            }
                        }
                        window.scrollTo(0, document.body.scrollHeight);
                    })();
                """

                run_config = CrawlerRunConfig(
                    extraction_strategy=llm_strategy,
                    cache_mode="BYPASS",
                    js_code=load_more_js,
                    delay_before_return_html=5.0,
                    page_timeout=60000,
                    magic=True,
                    flatten_shadow_dom=True,
                    max_retries=3,
                )

                log.info("Loading all articles for %s", original_url)
                result = await crawler.arun(url=original_url, config=run_config)

                if not result.success or not result.extracted_content:
                    log.error("Crawl4AI error: %s", result.error_message)
                    if result.markdown:
                        log.debug("Markdown length: %d", len(result.markdown))
                        log.debug("Markdown preview: %s", result.markdown[:500])
                    continue

                try:
                    all_articles = json.loads(result.extracted_content)
                    if not isinstance(all_articles, list):
                        all_articles = [all_articles] if all_articles else []
                except Exception as e:
                    log.error("JSON parsing error: %s", e)
                    log.debug("Raw content: %s", result.extracted_content)
                    continue

                if not all_articles:
                    log.warning("No articles found on %s", original_url)
                    continue

                raw_dates = [a.get("published_date", "") for a in all_articles]
                log.info("Extracted dates from LLM: %s", raw_dates)
                if any(d == "" for d in raw_dates):
                    log.info("Raw extracted content: %s", result.extracted_content)

                def parse_date_sort(a):
                    d = a.get("published_date", "")
                    try:
                        return datetime.strptime(d, "%Y-%m-%d").date()
                    except (ValueError, TypeError):
                        return date.min

                all_articles.sort(key=parse_date_sort, reverse=True)

                yesterday_articles = []
                for a in all_articles:
                    raw_date = a.get("published_date", "")
                    try:
                        article_date = datetime.strptime(raw_date, "%Y-%m-%d").date()
                    except (ValueError, TypeError):
                        continue
                    if article_date == yesterday:
                        yesterday_articles.append(a)

                if not yesterday_articles:
                    log.info("No articles for yesterday (%s) on %s", yesterday, original_url)
                    continue

                log.info("Found %d articles for yesterday (%s) (total loaded: %d)", len(yesterday_articles), yesterday, len(all_articles))

                for idx, article_data in enumerate(yesterday_articles):
                    raw_date = article_data.get("published_date", "")
                    article_date = datetime.strptime(raw_date, "%Y-%m-%d").date()

                    log.info("ARTICLE #%d Date: %s", idx + 1, article_date)
                    log.info("Title:  %s", article_data.get('title'))
                    log.info("Author: %s", article_data.get('author'))
                    if article_data.get('content'):
                        short_text = article_data.get('content')[:150].replace('\n', ' ')
                        log.info("Preview: %s...", short_text)
                    
                    verdicts = article_data.get("verdicts", [])
                    
                    for v in verdicts:
                        if v['is_relevant']:
                            if log_only:
                                log.info("Category: \"%s\" -> MATCH (log)", v['category_name'])
                            else:
                                log.info("Category: \"%s\" -> MATCH (saving to DB)", v['category_name'])
                                article_hash = generate_article_hash(
                                    target_feeds[0]['feed_id'],
                                    article_data.get('title') or '',
                                    raw_date,
                                    v['category_name']
                                )
                                
                                author = article_data.get('author', '')
                                if author:
                                    author = f";{author}"
                                
                                unix_date = int(datetime.strptime(raw_date, "%Y-%m-%d").timestamp())
                                
                                article_id = int(datetime.now().timestamp() * 1000000)
                                article_guid = article_data.get('title') or ''
                                article_link = original_url
                                
                                try:
                                    existing = await conn.fetchval(
                                        "SELECT id FROM public.admin_entry WHERE hash = $1",
                                        article_hash
                                    )
                                    
                                    if existing:
                                        log.info("Duplicate detected (hash: %s) - skipping", article_hash.hex()[:8])
                                        continue
                                    
                                    await conn.execute(
                                        """
                                        INSERT INTO public.admin_entry (id, guid, title, author, content, link, hash, id_feed, date)
                                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
                                        """,
                                        article_id,
                                        article_guid,
                                        article_data.get('title'),
                                        author,
                                        article_data.get('content'),
                                        article_link,
                                        article_hash,
                                        target_feeds[0]['feed_id'],
                                        unix_date
                                    )
                                    source_counts[original_url] += 1
                                except Exception as e:
                                    log.error("DB Error: %s", e)
                                    continue
                        else:
                            log.info("Category: \"%s\" -> REJECTED", v['category_name'])
    except Exception as e:
        log.error("Browser error: %s", e)
    finally:
        await conn.close()
        if source_counts:
            log.info("=" * 60)
            log.info("SUMMARY - Articles saved per source:")
            for url, count in sorted(source_counts.items(), key=lambda x: -x[1]):
                log.info("  %3d  %s", count, url)
            log.info("  %s", '-' * 40)
            log.info("  %3d  total", sum(source_counts.values()))
            log.info("=" * 60)
        if log_only:
            log.info("Done. Log-only mode - no data saved.")
        elif source_counts:
            log.info("Done. %d articles saved to DB.", sum(source_counts.values()))
        else:
            log.info("Done. No articles saved to DB.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Crawl and filter articles by category")
    parser.add_argument("--log-only", action="store_true", help="Only log results, do not save to DB")
    args = parser.parse_args()
    asyncio.run(main(log_only=args.log_only))
