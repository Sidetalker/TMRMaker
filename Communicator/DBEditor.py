import random
from com.ziclix.python.sql import zxJDBC

databaseLocation = 'jdbc:sqlite:/Users/sideslapd/Documents/Git Repos/TMRMaker/Knowledge Database/citygrid/LEIANiceness.db'

conn = zxJDBC.connect(databaseLocation, None, None, 'org.sqlite.JDBC')
c = conn.cursor()

## Remove current prices from the few restaurants that have it
#command = 'DELETE FROM cat_matching WHERE cat_id = 27 OR cat_id = 60 OR cat_id = 81'
#c.execute(command)
#
## Assign prices to all restaurants ($, $$, $$$)
#for x in range(1, 101):
#    val_one = x + 518
#
#    div = x % 3
#
#    if div == 2:
#        val_two = 27
#    if div == 1:
#        val_two = 60
#    if div == 0:
#        val_two = 81
#
#    command = 'INSERT INTO cat_matching (rowid, match_id, rest_id, cat_id) VALUES (%d, %d, %d, %d)' % (val_one, val_one, x, val_two)
#    c.execute(command)
#
#random.seed()
#
## Randomly assign ratings to all restaurants
#for x in range(1, 101):
#    my_rand = random.randint(1, 10)
#
#    command = 'UPDATE restaurant SET rating = %d WHERE rest_id = %d' % (my_rand, x)
#    c.execute(command)

the_dict = {}
the_dict['niceness'] = 0.75
the_dict['category'] = ['mexican', 'fast food']
the_dict['sorted'] = 'distance'
the_dict['info_query'] = {}
the_dict['info_query']['name'] = None
the_dict['info_query']['current'] = 1
the_dict['info_query']['values'] = ['hours', 'categories']

print the_dict
conn.commit()
