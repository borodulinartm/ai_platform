import asyncio
import importlib
import logging
import os
import signal
import sys
from datetime import datetime
from db_lock import DbLock

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger("scheduler")

sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')

CRON_MINUTE = int(os.environ.get("CRON_MINUTE", "5"))
HTTP_PORT = int(os.environ.get("HTTP_PORT", "8090"))
LOCK_CATEGORY = "ai_crawl_lock"

crawl_running = False
crawl_trigger = None
vibe_main = None
active_lock = None


async def release_lock():
    global active_lock
    if active_lock:
        try:
            await active_lock.release()
            log.info("Released lock '%s' on shutdown", LOCK_CATEGORY)
        except Exception:
            pass
        active_lock = None


def sync_release_lock():
    if active_lock:
        try:
            asyncio.run(release_lock())
        except Exception:
            pass


async def run_crawl(trigger="cron"):
    global crawl_running, crawl_trigger, vibe_main
    if crawl_running:
        log.info("Crawl already running locally, skipping")
        return
    crawl_running = True
    crawl_trigger = trigger
    await asyncio.sleep(0.5)

    if vibe_main is None:
        vibe_main = importlib.import_module("vibe-main")

    lock = DbLock(LOCK_CATEGORY)
    global active_lock
    active_lock = lock
    try:
        acquired = await lock.acquire()
        if not acquired:
            log.info("Lock '%s' is held by another instance, skipping", LOCK_CATEGORY)
            return
        log.info("=" * 60)
        log.info("Starting crawl [trigger=%s]", trigger)
        log.info("=" * 60)
        await vibe_main.main(log_only=False)
        log.info("Crawl finished [trigger=%s]", trigger)
    except Exception as e:
        log.error("Crawl error: %s", e)
    finally:
        try:
            await lock.release()
        except Exception:
            pass
        active_lock = None
        crawl_running = False
        crawl_trigger = None


async def handle_request(reader, writer):
    global crawl_running
    try:
        data = await reader.read(1024)
        if not data:
            return
        request_line = data.decode().split("\r\n")[0]
        method, path, _ = request_line.split(" ", 2)

        if method == "POST" and path == "/run":
            if crawl_running:
                body = b"Crawl already running"
                writer.write(b"HTTP/1.1 409 Conflict\r\nContent-Length: %d\r\n\r\n%s" % (len(body), body))
            else:
                asyncio.ensure_future(run_crawl(trigger="manual"))
                body = b"Crawl started"
                writer.write(b"HTTP/1.1 202 Accepted\r\nContent-Length: %d\r\n\r\n%s" % (len(body), body))
                await writer.drain()
                await writer.close()
                return
        elif method == "GET" and path == "/status":
            status = b"running" if crawl_running else b"idle"
            writer.write(b"HTTP/1.1 200 OK\r\nContent-Length: %d\r\n\r\n%s" % (len(status), status))
        else:
            writer.write(b"HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n")
        await writer.drain()
    except Exception:
        pass
    finally:
        writer.close()


async def cron_loop():
    log.info("Running every hour at :%02d", CRON_MINUTE)
    while True:
        now = datetime.now()
        target = now.replace(minute=CRON_MINUTE, second=0, microsecond=0)
        if now >= target:
            target = target.replace(hour=now.hour + 1) if now.hour < 23 else target.replace(hour=0)
            if now.hour == 23 and now.minute >= CRON_MINUTE:
                from datetime import timedelta
                target = (now + timedelta(days=1)).replace(minute=CRON_MINUTE, second=0, microsecond=0)
        wait_seconds = (target - now).total_seconds()
        if wait_seconds < 0:
            wait_seconds += 3600
        h, rem = divmod(int(wait_seconds), 3600)
        m, s = divmod(rem, 60)
        parts = []
        if h: parts.append(f"{h}h")
        if m: parts.append(f"{m}m")
        parts.append(f"{s}s")
        log.info("Next crawl at %s (in %s)", target.strftime("%H:%M:%S"), " ".join(parts))
        await asyncio.sleep(wait_seconds)
        await run_crawl(trigger="cron")


async def main():
    signal.signal(signal.SIGTERM, lambda *_: sync_release_lock())
    signal.signal(signal.SIGINT, lambda *_: sync_release_lock())

    if "--now" in sys.argv:
        await run_crawl(trigger="manual")
        return

    server = await asyncio.start_server(handle_request, "0.0.0.0", HTTP_PORT)
    log.info("HTTP API on port %d", HTTP_PORT)
    log.info("  POST /run    - trigger crawl manually")
    log.info("  GET  /status  - check crawl status")
    log.info("Use --now to run immediately")

    asyncio.ensure_future(cron_loop())
    async with server:
        await server.serve_forever()


if __name__ == "__main__":
    asyncio.run(main())
