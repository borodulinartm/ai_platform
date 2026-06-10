import asyncpg
import os

DB_CONFIG = {
    "user": os.environ.get("DB_USER") or "freshrss",
    "password": os.environ.get("DB_PASSWORD") or "freshrss",
    "database": os.environ.get("DB_NAME") or "freshrss",
    "host": os.environ.get("DB_HOST") or "183.87.45.129",
    "port": int(os.environ.get("DB_PORT") or "5432")
}


class DbLock:
    def __init__(self, category: str):
        self.category = category
        self.conn = None

    async def acquire(self) -> bool:
        self.conn = await asyncpg.connect(**DB_CONFIG)
        locked = await self.conn.fetchval(
            "SELECT locked FROM db_lock WHERE lock_type = $1",
            self.category
        )
        if locked:
            await self.conn.close()
            self.conn = None
            return False

        row = await self.conn.fetchrow(
            "SELECT * FROM db_lock WHERE lock_type = $1",
            self.category
        )
        if row is None:
            await self.conn.execute(
                "INSERT INTO db_lock (lock_type, locked) VALUES ($1, true)",
                self.category
            )
        else:
            await self.conn.execute(
                "UPDATE db_lock SET locked = true WHERE lock_type = $1",
                self.category
            )
        return True

    async def release(self):
        if self.conn:
            await self.conn.execute(
                "UPDATE db_lock SET locked = false WHERE lock_type = $1",
                self.category
            )
            await self.conn.close()
            self.conn = None

    async def __aenter__(self):
        acquired = await self.acquire()
        if not acquired:
            raise RuntimeError(f"Lock '{self.category}' is already held")
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        await self.release()
