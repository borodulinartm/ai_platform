import psycopg2
conn = psycopg2.connect('postgresql://postgres:admin@localhost:5432/test')
cur = conn.cursor()
cur.execute("""
    SELECT column_name, data_type, is_nullable 
    FROM information_schema.columns 
    WHERE table_name = 'admin_entry' 
    ORDER BY ordinal_position
""")
for row in cur.fetchall():
    print(row)
cur.close()
conn.close()
