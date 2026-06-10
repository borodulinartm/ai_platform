import asyncio
import importlib
import os
import sys
from datetime import datetime
from db_lock import DbLock

sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')

CRON_MINUTE = int(os.environ.get("CRON_MINUTE", "5"))
HTTP_PORT = int(os.environ.get("HTTP_PORT", "8090"))
LOCK_CATEGORY = "ai_crawl_lock"

crawl_running = False
vibe_main = None


async def run_crawl():
    global crawl_running, vibe_main
    if crawl_running:
        print("[scheduler] Crawl already running locally, skipping")
        return
    crawl_running = True

    if vibe_main is None:
        vibe_main = importlib.import_module("vibe-main")

    lock = DbLock(LOCK_CATEGORY)
    try:
        acquired = await lock.acquire()
        if not acquired:
            print(f"[scheduler] Lock '{LOCK_CATEGORY}' is held by another instance, skipping")
            return
        print(f"\n{'=' * 60}")
        print(f"[{datetime.now().isoformat()}] Starting crawl")
        print(f"{'=' * 60}")
        await vibe_main.main(log_only=False)
        print(f"[{datetime.now().isoformat()}] Crawl finished")
    except Exception as e:
        print(f"[-] Crawl error: {e}")
    finally:
        try:
            await lock.release()
        except Exception:
            pass
        crawl_running = False


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
                asyncio.ensure_future(run_crawl())
                body = b"Crawl started"
                writer.write(b"HTTP/1.1 202 Accepted\r\nContent-Length: %d\r\n\r\n%s" % (len(body), body))
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
    print(f"[scheduler] Running every hour at :{CRON_MINUTE:02d}")
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
        print(f"[scheduler] Next crawl at {target.isoformat()} (in {int(wait_seconds)}s)")
        await asyncio.sleep(wait_seconds)
        await run_crawl()


async def main():
    if "--now" in sys.argv:
        await run_crawl()
        return

    server = await asyncio.start_server(handle_request, "0.0.0.0", HTTP_PORT)
    print(f"[scheduler] HTTP API on port {HTTP_PORT}")
    print(f"[scheduler]   POST /run    - trigger crawl manually")
    print(f"[scheduler]   GET  /status  - check crawl status")
    print(f"[scheduler] Use --now to run immediately")

    asyncio.ensure_future(cron_loop())
    async with server:
        await server.serve_forever()


if __name__ == "__main__":
    asyncio.run(main())
