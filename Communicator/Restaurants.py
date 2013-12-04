__author__ = 'kevin'

import math
import urllib2
from com.xhaus.jyson import JysonCodec as json
from com.ziclix.python.sql import zxJDBC
from pprint import pprint

databaseLocation = 'jdbc:sqlite:LEIANiceness.db'


class Address:
    postal_code = -1
    state = 'none'
    street = 'none'
    city = 'none'
    latitude = 0.01
    longitude = 0.01
    latitude_goog = 0.01
    longitude_goog = 0.01
    queried = False
    formatted_name = 'none'

    def __init__(self, street_i, city_i, zip_i, state_i, lat_i, long_i):
        self.street = street_i
        self.city = city_i
        self.postal_code = zip_i
        self.state = state_i
        self.latitude = lat_i
        self.longitude = long_i
        self.latitude_goog = 0.01
        self.longitude_goog = 0.01
        self.queried = False
        self.formatted_name = 'none'

    def query_google(self):
        print self.postal_code
        request_string = ('http://maps.googleapis.com/maps/api/geocode/json?address='
                          '%s,%s,%s&sensor=false') % (self.street.replace(' ', '+'), self.postal_code, self.state)

        response = urllib2.urlopen(request_string)
        json_response = response.read()
        dict_response = json.loads(json_response)

        self.formatted_name = dict_response['results'][0]['formatted_address']
        self.latitude_goog = dict_response['results'][0]['geometry']['location']['lat']
        self.longitude_goog = dict_response['results'][0]['geometry']['location']['lng']

        self.queried = True

    def __repr__(self):
        return "%s %s, %s %s" % (self.street, self.city, self.state, self.postal_code)

class User:
    id = -1
    latitude = 0.1
    longitude = 0.1

    def __init__(self, id_i):
        self.id = id_i
        self.query()

    def query(self):
        conn = zxJDBC.connect(databaseLocation, None, None, 'org.sqlite.JDBC')
        c = conn.cursor()

        safe_id = (self.id,)
        c.execute('SELECT location.latitude, location.longitude FROM location location, '
                  'human human WHERE location.loc_id = human.home_addr AND human.human_id = ?', safe_id)
        response = c.fetchall()
        self.latitude = response[0][0]
        self.longitude = response[0][1]

        c.close()
        conn.close()


class Restaurant:
    id = -1
    name = 'blank'
    rating = -1
    phone = 'none'
    is_open = -1
    location = 'none'
    rough_distance = 999.9
    exact_distance = 999.9
    categories = []
    niceness = -1

    def __init__(self, query_list):
        self.id = query_list[0]
        self.name = query_list[1]
        self.rating = query_list[2]
        self.phone = query_list[3]
        self.is_open = query_list[4]
        self.location = Address(query_list[8], query_list[5],
                                query_list[6], query_list[7],
                                query_list[9], query_list[10])
        self.categories = []
        self.niceness = 0

    def compute_niceness(self):
        nice = 0
        if '$$' in self.categories:
            nice += 3
        if '$$$' in self.categories:
            nice += 6
        nice += self.rating
        nice /= float(16)
        self.niceness = nice

    def compute_rough_distance(self, the_user):
        self.rough_distance = lat_long_distance(self.location.latitude, self.location.longitude,
                                                the_user.latitude, the_user.longitude)

    def compute_exact_distance(self, the_user):
        self.exact_distance = route_distance(self.location.latitude, self.location.longitude,
                                                the_user.latitude, the_user.longitude)

    def print_me(self):
        print self.name
        print '\trest_id:', self.id
        print '\tphone number:', self.phone
        print '\trating:', self.rating
        print '\tniceness: %.2f' % self.niceness
        print '\tis_open:', self.is_open
        print '\trough_distance: %.2f' % self.rough_distance
        if self.exact_distance != 999.9:
            print '\texact_distance: %.2f' % self.exact_distance
        print '\tcategories:'

        for cat in self.categories:
            print '\t\t', cat


class RestaurantGroup:
    restaurants = []
    filtered_restaurants = []
    current_user = None

    arg_niceness = False
    niceness = 0.0
    arg_category = False
    category = None

    def __init__(self, the_user):
        self.restaurants = []
        self.filtered_restaurants = []
        self.arg_niceness = False
        self.niceness = 0.0
        self.arg_category = False
        self.category = None
        self.current_user = the_user

    def populate(self):
        conn = zxJDBC.connect(databaseLocation, None, None, 'org.sqlite.JDBC')
        c = conn.cursor()

        c.execute('SELECT restaurant.rest_id, restaurant.rest_name, restaurant.rating, '
                  'restaurant.phone, restaurant.open, location.city, location.postal_code, '
                  'location.state, location.street, location.latitude, location.longitude '
                  'FROM restaurant restaurant, location location WHERE '
                  'restaurant.rest_location = location.loc_id')
        for row in c.fetchall():
            self.restaurants.append(Restaurant(row))
        for rest in self.restaurants:
            safe_rest = (rest.id,)
            c.execute('SELECT categories.cat_name, restaurant.rest_name FROM cat_matching '
                      'cat_matching, categories categories, restaurant restaurant WHERE '
                      'cat_matching.rest_id = restaurant.rest_id AND cat_matching.cat_id '
                      '= categories.cat_id AND restaurant.rest_id = ''?''', safe_rest)
            for row in c.fetchall():
                rest.categories.append(row[0])

            rest.compute_niceness()
            rest.compute_rough_distance(self.current_user)

        self.filtered_restaurants = self.restaurants
        self.restaurants[0].compute_exact_distance(self.current_user)

        c.close()
        conn.close()

    def narrow(self, what, query):
        if what == 'category':
            self.arg_category = True
            self.category = query

        if what == 'niceness':
            self.arg_niceness = True
            self.niceness = query

        self.filtered_restaurants = self.restaurants

        if self.arg_category:
            self.filtered_restaurants = [x for x in self.filtered_restaurants if self.category in x.categories]

        if self.arg_niceness:
            self.filtered_restaurants = [x for x in self.filtered_restaurants if x.niceness > self.niceness]

    def hist_dict(self):
        the_dict = {}

        if self.arg_niceness:
            the_dict['niceness'] = self.niceness

        if self.arg_category:
            the_dict['category'] = self.category

        return the_dict

    def print_me(self):
        print 'Requirements:'
        if self.arg_category:
            print 'Category: %s' % self.category
        if self.arg_niceness:
            print 'Niceness > %.2f' % self.niceness

        print 'Restaurants:'
        for rest in self.filtered_restaurants:
            rest.print_me()

    def sort_output(self, parameter):
        if parameter == 'niceness':
            sorted_restaurants = sorted(self.filtered_restaurants, key=lambda restaurant:
                                        restaurant.niceness, reverse=True)
            self.filtered_restaurants = sorted_restaurants
        if parameter == 'distance':
            rough_sort = sorted(self.filtered_restaurants, key=lambda restaurant:
                                restaurant.rough_distance)

            count = 5
            if len(rough_sort) < 5:
                count = len(rough_sort)

            for index in range(count):
                rough_sort[index].compute_exact_distance(self.current_user)

            exact_sort = sorted(rough_sort, key=lambda restaurant:
                                restaurant.exact_distance)

            self.filtered_restaurants = exact_sort

class TMRProcessor:
    restaurant_group = None
    current_user = None

    def __init__(self):
        self.current_user = User(1)
        self.restaurant_group = RestaurantGroup(self.current_user)
        self.restaurant_group.populate()

    def process_tmr(self, tmr, queries):
        desired_niceness = None
        desired_type = None

        if 'RESTAURANT-0' in tmr:
            if 'niceness' in tmr['RESTAURANT-0']:
                desired_niceness = float(tmr['RESTAURANT-0']['niceness'][1:])

        if 'CATEGORY-0' in tmr:
            if 'type' in tmr['CATEGORY-0']:
                desired_type = tmr['CATEGORY-0']['type']

        for what, value in queries.iteritems():
            if what == 'niceness':
                self.restaurant_group.narrow('niceness', value)
            if what == 'category':
                self.restaurant_group.narrow('category', value)

        if desired_niceness is not None:
            self.restaurant_group.narrow('niceness', desired_niceness)

        if desired_type is not None:
            self.restaurant_group.narrow('category', desired_type)

        return self.restaurant_group.hist_dict()


def lat_long_distance(lat1, long1, lat2, long2):
    # Convert latitude and longitude to
    # spherical coordinates in radians.
    degrees_to_radians = math.pi / 180.0

    # phi = 90 - latitude
    phi1 = (90.0 - lat1) * degrees_to_radians
    phi2 = (90.0 - lat2) * degrees_to_radians

    # theta = longitude
    theta1 = long1 * degrees_to_radians
    theta2 = long2 * degrees_to_radians

    # Use haversine formula to calculate distance
    cos = (math.sin(phi1) * math.sin(phi2) *
           math.cos(theta1 - theta2) +
           math.cos(phi1) * math.cos(phi2))
    arc = math.acos(cos)

    # Multiply by radius of Earth for final result
    dist = arc * 3960

    return dist


def route_distance(lat1, long1, lat2, long2):
    request_string = ('http://maps.googleapis.com/maps/api/distancematrix/json?origins='
                      '%f,%f&destinations=%f,%f&mode=driving&sensor=false') % (lat1, long1, lat2, long2)

    response = urllib2.urlopen(request_string)
    json_response = response.read()
    dict_response = json.loads(json_response)

    meters = dict_response['rows'][0]['elements'][0]['distance']['value']
    miles = meters * 0.000621371

    return miles


# tmr1 = {'SPECIFY-TIME-0': {u'time': 'TIME-1'}, 'HUMAN-1': {u'gender': '"male"'}, 'HUMAN-0': {}, 'TIME-1': {u'minute': '0', u'dayOfMonth': '-1', u'pm': '-1', u'dayOfWeek': '-1', u'hour': '7', u'month': '-1'}, 'TIME-0': {u'minute': '-1', u'dayOfMonth': '25', u'pm': '-1', u'dayOfWeek': '2', u'hour': '-1', u'month': '10'}, 'RESTAURANT-0': {u'niceness': .75}, 'LOOK-FOR-0': {u'theme': 'RESTAURANT-0'}, 'MODALITY-0': {u'attribituted-to': 'HUMAN-0', u'scope': 'LOOK-FOR-0', u'type': '"volitive"', u'value': '1'}}
#tmr2 = {'HUMAN-0': {u'taste': '[CATEGORY-0]'}, 'CATEGORY-0': {u'type': 'mexican'}}
#
# processor = TMRProcessor()
# query_dict = processor.process_tmr(tmr1, {})
# processor.restaurant_group.sort_output('distance')
# processor.restaurant_group.print_me()
#pprint(query_dict)
#query_dict = processor.process_tmr(tmr2, query_dict)
#pprint(query_dict)
#processor.restaurant_group.print_me()

# NEXT TIME
# Sort output by niceness
# Too many outputs
# Get distance for top few - maybe able to get all in one query?
# Ask user for importance of distance
# Get hours data
