import asyncpg
import asyncio

DB_CONFIG = {
    "user": "freshrss",
    "password": "freshrss",
    "database": "freshrss",
    "host": "183.87.45.129",
    "port": 5432
}

async def check_table():
    conn = await asyncpg.connect(**DB_CONFIG)
    
    # Check all columns and defaults
    result = await conn.fetch("""
        SELECT column_name, data_type, column_default, is_nullable 
        FROM information_schema.columns 
        WHERE table_name = 'admin_entry' 
        ORDER BY ordinal_position
    """)
    
    print("admin_entry columns:")
    for row in result:
        print(f"  {row['column_name']} ({row['data_type']}) default={row['column_default']} nullable={row['is_nullable']}")
    
    # Check sequences
    seqs = await conn.fetch("""
        SELECT sequence_name
        FROM information_schema.sequences
        WHERE sequence_schema = 'public'
    """)
    print("\nSequences:")
    for s in seqs:
        print(f"  {s['sequence_name']}")
    
    # Check max id
    max_id = await conn.fetchval("SELECT MAX(id) FROM admin_entry")
    print(f"\nMax id in admin_entry: {max_id}")
    
    await conn.close()

if __name__ == "__main__":
    asyncio.run(check_table())
