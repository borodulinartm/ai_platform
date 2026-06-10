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
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
os.environ['PYTHONIOENCODING'] = 'utf-8'
sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')

litellm.suppress_debug_info = True


def generate_article_hash(feed_id: int, title: str, published_date: str, category_name: str) -> bytes:
    """Generate unique hash for article to detect duplicates."""
    content = f"{feed_id}:{title}:{published_date}:{category_name}"
    return hashlib.md5(content.encode()).digest()

# ==========================================
# POSTGRESQL CONNECTION CONFIG
# ==========================================
DB_CONFIG = {
    "user": os.environ.get("DB_USER", "freshrss"),
    "password": os.environ.get("DB_PASSWORD", "freshrss"),
    "database": os.environ.get("DB_NAME", "freshrss"),
    "host": os.environ.get("DB_HOST", "183.87.45.129"),
    "port": int(os.environ.get("DB_PORT", "5432"))
}

# ==========================================
# 1. PYDANTIC SCHEMA FOR MULTI-CATEGORIZATION
# ==========================================
class CategoryVerdict(BaseModel):
    category_name: str = Field(description="The EXACT name of the category being evaluated from the provided list.")
    is_relevant: bool = Field(description="TRUE if the article strictly matches this specific category's description. Otherwise FALSE.")

class MultiFilteredArticle(BaseModel):
    title: str = Field(description="Title of the article. Leave empty only if ALL categories are irrelevant.")
    author: str = Field(description="Author name, leave empty if not found.")
    content: str = Field(description="Full text content of the article clean of HTML. Leave empty only if ALL categories are irrelevant.")
    published_date: str = Field(description="Publication date of the article in YYYY-MM-DD format.")
    verdicts: list[CategoryVerdict] = Field(description="A list of verdicts for EVERY category requested in the prompt.")


# ==========================================
# 2. MAIN ASYNC PROCESS
# ==========================================
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
        print("[-] No matching 'scraped::' sources found in admin_feed.")
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
    print(f"[*] {mode_str} DB records: {len(rows)}. Unique URLs to check: {len(url_groups)}")     

    yesterday = date.today() - timedelta(days=1)
    source_counts = defaultdict(int)

    try:
        browser_config = BrowserConfig(
            headless=True,
            verbose=False,
        )
        async with AsyncWebCrawler(config=browser_config) as crawler:
            for original_url, target_feeds in url_groups.items():
                print(f"\n" + "="*60)
                print(f"[*] SCANNING: {original_url}")
                print(f"[*] Checking categories: {[f['cat_name'] for f in target_feeds]}")
                print("="*60)
                
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
                llm_provider = os.environ.get("LLM_PROVIDER", "deepseek")
                llm_model = os.environ.get("LLM_MODEL", "deepseek-v4-flash")
                llm_config = LLMConfig(
                    provider=f"openrouter/{llm_provider}/{llm_model}",
                    api_token=os.environ.get("LLM_KEY", ""),
                    base_url=os.environ.get("LLM_URL", "https://openrouter.ai/api/v1")
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

                print(f"[*] Loading all articles for {original_url}")
                result = await crawler.arun(url=original_url, config=run_config)

                if not result.success or not result.extracted_content:
                    print(f"[-] Crawl4AI error: {result.error_message}")
                    if result.markdown:
                        md_len = len(result.markdown)
                        print(f"[DEBUG] Markdown length: {md_len}")
                        print(f"[DEBUG] Markdown preview: {result.markdown[:300]}")
                    continue

                try:
                    all_articles = json.loads(result.extracted_content)
                    if not isinstance(all_articles, list):
                        all_articles = [all_articles] if all_articles else []
                except Exception as e:
                    print(f"[-] JSON parsing error: {e}")
                    print(f"[RAW] {result.extracted_content[:500]}")
                    continue

                if not all_articles:
                    print(f"[-] No articles found on {original_url}")
                    continue

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
                    print(f"[-] No articles for yesterday ({yesterday}) on {original_url}")
                    continue

                print(f"[*] Found {len(yesterday_articles)} articles for yesterday ({yesterday}) (total loaded: {len(all_articles)})")

                for idx, article_data in enumerate(yesterday_articles):
                    raw_date = article_data.get("published_date", "")
                    article_date = datetime.strptime(raw_date, "%Y-%m-%d").date()

                    print(f"\n  [ARTICLE #{idx+1}] Date: {article_date}")
                    print(f"  [DATA] Title:  {article_data.get('title')}")
                    print(f"  [DATA] Author: {article_data.get('author')}")
                    if article_data.get('content'):
                        short_text = article_data.get('content')[:150].replace('\n', ' ')
                        print(f"  [DATA] Preview: {short_text}...")
                    
                    print("\n  [AI VERDICTS]:")
                    verdicts = article_data.get("verdicts", [])
                    
                    for v in verdicts:
                        if v['is_relevant']:
                            if log_only:
                                print(f"    -> Category: \"{v['category_name']}\" -> ✅ MATCH (log)")
                            else:
                                print(f"    -> Category: \"{v['category_name']}\" -> ✅ MATCH (saving to DB)")
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
                                        print(f"    -> Duplicate detected (hash: {article_hash.hex()[:8]}) - skipping")
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
                                    print(f"    -> DB Error: {e}")
                                    continue
                        else:
                            print(f"    -> Category: \"{v['category_name']}\" -> ❌ REJECTED (skip)")
    except Exception as e:
        print(f"[-] Browser error: {e}")
    finally:
        await conn.close()
        if source_counts:
            print("\n" + "=" * 60)
            print("[SUMMARY] Articles saved per source:")
            for url, count in sorted(source_counts.items(), key=lambda x: -x[1]):
                print(f"  {count:>3}  {url}")
            print(f"  {'─' * 40}")
            print(f"  {sum(source_counts.values()):>3}  total")
            print("=" * 60)
        if log_only:
            print("\n[*] Done. Log-only mode — no data saved.")
        elif source_counts:
            print(f"\n[*] Done. {sum(source_counts.values())} articles saved to DB.")
        else:
            print("\n[*] Done. No articles saved to DB.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Crawl and filter articles by category")
    parser.add_argument("--log-only", action="store_true", help="Only log results, do not save to DB")
    args = parser.parse_args()
    asyncio.run(main(log_only=args.log_only))
