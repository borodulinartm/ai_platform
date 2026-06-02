import asyncpg
import asyncio
import os
import sys

os.environ['PYTHONIOENCODING'] = 'utf-8'
sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')

DB_CONFIG = {
    "user": "freshrss",
    "password": "freshrss",
    "database": "freshrss",
    "host": "183.87.45.129",
    "port": 5432
}

async def check_records():
    conn = await asyncpg.connect(**DB_CONFIG)
    
    result = await conn.fetch("""
        SELECT id, title, author, date 
        FROM admin_entry 
        ORDER BY id DESC 
        LIMIT 5
    """)
    
    print("Latest 5 records:")
    for row in result:
        print(f"  {row['id']} | {row['title'][:60]} | {row['author'][:30]} | {row['date']}")
    
    await conn.close()

if __name__ == "__main__":
    asyncio.run(check_records())
