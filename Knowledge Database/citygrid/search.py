import json
import sqlite3
import urllib
import urllib2
import time

url_params = {}
url_params['type'] = 'restaurant'
url_params['where'] = 'Troy,NY'
url_params['publisher'] = 'test'
url_params['format'] = 'json'
url_params['rpp'] = '50'

host = 'api.citygridmedia.com/content/places/v2'
where_path = '/search/where'
detail_path = '/detail'
encoded_params = urllib.urlencode(url_params)

url = 'http://%s%s?%s' %(host,where_path,encoded_params)
print 'URL: %s' % (url,)

while True:
	try:
		conn = urllib2.urlopen(url, None)
		break
	except urllib2.URLError:
		print 'connecting failed'
		time.sleep(5)

print 'got the list of restaurants'

response = json.loads(conn.read())
conn.close()

conn = sqlite3.connect('LEIA.db')
c = conn.cursor()

locations = response['results']['locations']
for restaurant in locations:
	address = restaurant['address']
	c.execute('''insert into location values(NULL, ?, ?, ?, ?, ?, ?)''', (address['city'], address['postal_code'], address['state'], address['street'], restaurant['latitude'], restaurant['longitude']))
	c.execute('''insert into restaurant values(NULL, ?, ?, last_insert_rowid(), ?, ?)''', (restaurant['name'], restaurant['rating'], restaurant['phone_number'], restaurant['business_operation_status']))
	c.execute('''select last_insert_rowid()''')
	rest_id = c.fetchone()[0]

	url_params = {}
	url_params['id'] = restaurant['id']
	url_params['id_type'] = 'cs'
	url_params['publisher'] = 'test'
	url_params['client_ip'] = '123.124.123.124'
	url_params['format'] = 'json'
	encoded_params = urllib.urlencode(url_params)
	url = 'http://%s%s?%s' %(host,detail_path,encoded_params)
	while True:
		try:
			connect = urllib2.urlopen(url,None)
			break
		except urllib2.URLError:
			print '  connecting failed'
			time.sleep(10)
	details = json.loads(connect.read())
	connect.close()

	det = details['locations'][0]
	cat = det['categories']

	for entry in cat:
		c.execute('''select * from categories where cat_name=?''', (entry['parent'],))
		parent = c.fetchone()
		if parent == None:
			c.execute('''insert into categories (cat_id, cat_name) values(NULL, ?)''', (entry['parent'],))
			c.execute('''select last_insert_rowid()''')
			parent_id = c.fetchone()[0]
		else:
			parent_id = parent[0]
		
		c.execute('''select * from categories where cat_name=?''',(entry['name'],))
		temp_entry = c.fetchone()
		if temp_entry == None:
			c.execute('''insert into categories values(NULL, ?, ?, ?)''',(entry['name'], parent_id, entry['parent']))
			c.execute('''select last_insert_rowid()''')
			cat_id = c.fetchone()[0]
		elif temp_entry[2] == 'NULL':
			c.execute('''update categories set parent_id=?, parent_name=? where cat_id=?''', (parent_id, entry['parent'], cat_id))
			c.execute('''select last_insert_rowid()''')
			cat_id = c.fetchone()[0]
		else:
			cat_id = temp_entry[0]
		c.execute('''insert into cat_matching values(NULL, ?, ?)''', (rest_id, cat_id))
	time.sleep(5)
		
c.execute('''select * from location''')
print 'locations:'
for i in c:
	print "  "+str(i)
print ''

c.execute('''select * from restaurant''')
print 'restaurants:'
for i in c:
	print "  "+str(i)
print ''

print 'categories:'
for i in c.execute('''select * from categories'''):
	print "  "+str(i)
print ''

print 'matchings:'
for i in c.execute('''select * from cat_matching'''):
	print "  "+str(i)
print ''

conn.commit()
conn.close()
#print json.dumps(response, sort_keys=True, indent=2)
