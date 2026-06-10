import asyncio
import importlib
import logging
import os
import sys
import threading
from datetime import datetime
from http.server import HTTPServer, BaseHTTPRequestHandler
from apscheduler.schedulers.background import BackgroundScheduler

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')

vibe_main = importlib.import_module("vibe-main")
from db_lock import DbLock

CRON_MINUTE = int(os.environ.get("CRON_MINUTE", "5"))
HTTP_PORT = int(os.environ.get("HTTP_PORT", "8090"))
LOCK_CATEGORY = "ai_crawl_lock"

crawl_running = False


def run_crawl():
    global crawl_running
    if crawl_running:
        print("[scheduler] Crawl already running locally, skipping")
        return
    crawl_running = True

    lock = DbLock(LOCK_CATEGORY)
    try:
        acquired = asyncio.run(lock.acquire())
        if not acquired:
            print(f"[scheduler] Lock '{LOCK_CATEGORY}' is held by another instance, skipping")
            return
        print(f"\n{'=' * 60}")
        print(f"[{datetime.now().isoformat()}] Starting crawl")
        print(f"{'=' * 60}")
        asyncio.run(vibe_main.main(log_only=False))
        print(f"[{datetime.now().isoformat()}] Crawl finished")
    except Exception as e:
        print(f"[-] Crawl error: {e}")
    finally:
        try:
            asyncio.run(lock.release())
        except Exception:
            pass
        crawl_running = False


class CrawlHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path == "/run":
            if crawl_running:
                self.send_response(409)
                self.end_headers()
                self.wfile.write(b"Crawl already running")
            else:
                threading.Thread(target=run_crawl, daemon=True).start()
                self.send_response(202)
                self.end_headers()
                self.wfile.write(b"Crawl started")
        else:
            self.send_response(404)
            self.end_headers()

    def do_GET(self):
        if self.path == "/status":
            self.send_response(200)
            self.end_headers()
            status = "running" if crawl_running else "idle"
            self.wfile.write(status.encode())
        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format, *args):
        pass


if __name__ == "__main__":
    if "--now" in sys.argv:
        run_crawl()
    else:
        scheduler = BackgroundScheduler()
        scheduler.add_job(run_crawl, 'cron', minute=CRON_MINUTE)
        scheduler.start()

        server = HTTPServer(("0.0.0.0", HTTP_PORT), CrawlHandler)
        print(f"[scheduler] Running every hour at :{CRON_MINUTE:02d}")
        print(f"[scheduler] HTTP API on port {HTTP_PORT}")
        print(f"[scheduler]   POST /run    - trigger crawl manually")
        print(f"[scheduler]   GET  /status  - check crawl status")
        print(f"[scheduler] Use --now to run immediately")

        try:
            server.serve_forever()
        except KeyboardInterrupt:
            scheduler.shutdown()
            server.shutdown()
