import sqlite3

conn = sqlite3.connect('LEIA.db')
c = conn.cursor()

c.execute('''delete from location''')
c.execute('''delete from restaurant''')
c.execute('''delete from categories''')
c.execute('''delete from cat_matching''')

conn.commit()
conn.close()
