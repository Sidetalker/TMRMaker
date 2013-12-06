__author__ = 'kevin'

import math
import urllib2
from com.xhaus.jyson import JysonCodec as json
from com.ziclix.python.sql import zxJDBC

# Location of the knowledge base
databaseLocation = 'jdbc:sqlite:/Users/sideslapd/Documents/Git Repos/TMRMaker/Knowledge Database/citygrid/LEIANiceness.db'

# Variables for niceness computation
cat_pref_boost = 1.4
cost_boost = 6.7
rating_multiplier = 1.1

query_limit = 5  # How many distance queries should we make to Google


# Simple class to store address information
# Includes function to fetch more information
# About the current address from Google
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

    # Query google using the information provided
    # When the class instance was instantiated
    # Stores the formatted name, latitude and longitude
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


# Class for users referenced in the TMR
# Data is obtained from the SQLite knowledge base
# Designed to be easily expandable
class User:
    id = -1
    latitude = 0.1
    longitude = 0.1
    cat_prefs = []

    # Initialize a user using their id number
    def __init__(self, id_i):
        self.id = id_i
        self.query()

    # Query the database for location and preference information
    def query(self):
        conn = zxJDBC.connect(databaseLocation, None, None, 'org.sqlite.JDBC')
        cursor = conn.cursor()

        # Request location information from the database
        safe_id = (self.id,)  # Prevent SQL injection attacks
        cursor.execute('SELECT location.latitude, location.longitude FROM location location, '
                       'human human WHERE location.loc_id = human.home_addr AND human.human_id = ?', safe_id)
        response = cursor.fetchall()
        self.latitude = response[0][0]
        self.longitude = response[0][1]

        # Request category preference information from the database
        cursor.execute('SELECT categories.cat_name FROM pref_cuisine pref_cuisine, '
                       'categories categories WHERE pref_cuisine.cat_id = '
                       'categories.cat_id AND pref_cuisine.human_id = ?', safe_id)
        response = cursor.fetchall()
        self.cat_prefs = []
        for item in response:
            item_string = str(item[0])
            self.cat_prefs.append(item_string)

        cursor.close()
        conn.close()


    # Add a new user preference to the database
    def add_preference(self, preference):
        conn = zxJDBC.connect(databaseLocation, None, None, 'org.sqlite.JDBC')
        cursor = conn.cursor()

        safe_preference = (preference,)

        # Lookup the corresponding category ID
        cursor.execute('SELECT cat_id FROM categories WHERE cat_name = ''?''', safe_preference)

        response = cursor.fetchall()
        if len(response) == 0:
            return

        cat_id = response[0]
        query_string = '%d, %d' % (self.id, cat_id)
        safe_query_string = (query_string,)

        # Insert the category ID into the preferences table
        cursor.execute('INSERT INTO pref_cuisine (?) VALUES (1, 1000)', safe_query_string)

        conn.commit()
        cursor.close()
        conn.close()


# Similar to the User class, this class stores
# Restaurant information acquired from the knowledge base
# Provides functionality to calculate distances and compute niceness
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
    schedule_days = []
    schedule_starts = []
    schedule_ends = []
    niceness = -1
    fit_rating = 0

    # Initialize directly from SQL query response
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
        self.schedule_days = []
        self.schedule_starts = []
        self.schedule_ends = []
        self.niceness = 0
        self.fit_rating = 0

    def compute_fit_rating(self, req_cats):
        for category in req_cats:
            if category in self.categories:
                self.fit_rating += 1

        if len(req_cats) > 0:
            self.fit_rating /= float(len(req_cats))
        else:
            self.fit_rating = 0

    # Compute niceness from rating, price and category preferences
    # Category preferences are loaded from user preferences which can
    # Be updated during runtime as the user expresses more preferences
    def compute_niceness(self, user_info):
        nice = 0
        cat_prefs = user_info.cat_prefs
        cat_prefs_count = len(cat_prefs)

        # If there are no preferences all
        # Restaurants will receive full boost points
        if cat_prefs_count == 0:
            nice += cat_pref_boost
        else:
            # Apply points based on categories
            points_per_cat = cat_pref_boost / cat_prefs_count

            for category in cat_prefs:
                if category in self.categories:
                    nice += points_per_cat

        # Apply points based on prices
        if '$$' in self.categories:
            nice += cost_boost / 2
        if '$$$' in self.categories:
            nice += cost_boost

        # Apply points based on ratings
        nice += self.rating * rating_multiplier

        # Calculate niceness from 0-1 and save the value
        nice /= float(cat_pref_boost + cost_boost + rating_multiplier * 10)
        self.niceness = nice

    # Computes and saves the "as the bird flies" distance
    # Between the provided user location and the restaurant location
    def compute_rough_distance(self, the_user):
        self.rough_distance = lat_long_distance(self.location.latitude, self.location.longitude,
                                                the_user.latitude, the_user.longitude)

    # Computes and saves the route distance provided from Google
    # Between the provided user location and the restaurant location
    def compute_exact_distance(self, the_user):
        self.exact_distance = route_distance(self.location.latitude, self.location.longitude,
                                             the_user.latitude, the_user.longitude)

    # Prints restaurant data to console - used for debugging
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


# This class contains a group of restaurants and a user of the group
# It provides functions for sorting restaurants by Restaurant variables and
# Narrowing down the list by Restaurant variables. The filter variables used
# To narrow the list are stored and can be modified at any time
class RestaurantGroup:
    restaurants = []
    filtered_restaurants = []
    current_user = None

    arg_niceness = False
    niceness = 0.0
    arg_category = 0
    categories = []
    sorted_by = None
    query_current = 0
    query_name = None
    query_var = None
    query_values = []

    def __init__(self, the_user):
        self.restaurants = []
        self.filtered_restaurants = []
        self.arg_niceness = False
        self.niceness = 0.0
        self.arg_category = 0
        self.categories = []
        self.current_user = the_user
        self.sorted_by = None
        self.query_current = 0
        self.query_var = None
        self.query_name = None
        self.query_values = []
        self.populate()

    # Called at initialization this function queries the database for all
    # Restaurant information and creates a Restaurant object for each of them
    # "As the bird flies" distance and a current niceness calculated immediately
    # route distance is calculated only when needed since it requires a query to Google
    def populate(self):
        conn = zxJDBC.connect(databaseLocation, None, None, 'org.sqlite.JDBC')
        cursor = conn.cursor()

        # Get general restaurant information
        cursor.execute('SELECT restaurant.rest_id, restaurant.rest_name, restaurant.rating, '
                       'restaurant.phone, restaurant.open, location.city, location.postal_code, '
                       'location.state, location.street, location.latitude, location.longitude '
                       'FROM restaurant restaurant, location location WHERE '
                       'restaurant.rest_location = location.loc_id')
        for row in cursor.fetchall():
            self.restaurants.append(Restaurant(row))

        # Get more involved restaurant information
        for rest in self.restaurants:
            safe_rest = (rest.id,)

            # Get restaurants categories
            cursor.execute('SELECT categories.cat_name, restaurant.rest_name FROM cat_matching '
                           'cat_matching, categories categories, restaurant restaurant WHERE '
                           'cat_matching.rest_id = restaurant.rest_id AND cat_matching.cat_id '
                           '= categories.cat_id AND restaurant.rest_id = ''?''', safe_rest)
            for row in cursor.fetchall():
                rest.categories.append(row[0])

            # Get restaurant times
            cursor.execute('SELECT dow, start_time, end_time FROM schedule WHERE rest_id = ? ', safe_rest)
            for row in cursor:
                rest.schedule_days.append(row[0])
                rest.schedule_starts.append(row[1])
                rest.schedule_ends.append(row[2])

            # Computer niceness and rough distance
            rest.compute_niceness(self.current_user)
            rest.compute_rough_distance(self.current_user)

        # Initialize the filtered restaurant list
        self.filtered_restaurants = self.restaurants

        cursor.close()
        conn.close()

    # This function narrows the current restaurant list by a
    # Niceness value, a set of categories or both
    def narrow(self, what, query, remove):
        # Add/remove a category restriction
        if what == 'category':
            if remove:
                if self.arg_category != 0 and query in self.categories:
                    self.arg_category -= 1
                    self.categories = [x for x in self.categories if query != x]
            else:
                self.arg_category += 1
                self.categories.append(query)
                #print self.arg_category
                #print 'ASFHSDF'
                #if self.arg_category is not 0:
                #    print self.arg_category
                #    print 'HWEFJAI'
                #    if len(self.arg_category) != 0:
                #        self.arg_category += 1
                #        self.categories.append(query)

        # Add/remove a niceness restriction
        if what == 'niceness':
            if remove:
                self.arg_niceness = False
            else:
                self.arg_niceness = True
                self.niceness = query

        # Reset the list of filtered restaurants
        self.filtered_restaurants = self.restaurants

        # Get information for a single named restaurant
        if what == 'info':
            self.filtered_restaurants = [x for x in self.filtered_restaurants
                                         if x.name == self.query_name]
            return

        # Filter by category
        if self.arg_category >= 1:
            self.filtered_restaurants = [x for x in self.filtered_restaurants if
                                         self.has_category(x.categories)]

        # Filter by niceness
        if self.arg_niceness:
            self.filtered_restaurants = [x for x in self.filtered_restaurants
                                         if x.niceness > self.niceness]

    # Helper function for category filtering
    def has_category(self, rest_cats):
        for cat in self.categories:
            if cat in rest_cats:
                return True

        return False

    # Returns current information in a dictionary
    def hist_dict(self):
        the_dict = {'niceness': self.niceness, 'category': self.categories,
                    'sorted': self.sorted_by, 'info_query': {}}
        the_dict['info_query']['name'] = self.query_name
        the_dict['info_query']['current'] = self.query_current
        the_dict['info_query']['values'] = self.query_values

        return the_dict

    # Print current restrictions and filtered restaurants
    def print_me(self):
        print 'Requirements:'
        if self.arg_category >= 1:
            print 'Categories (%d):' % self.arg_category
            print self.categories
        if self.arg_niceness:
            print 'Niceness > %.2f' % self.niceness

        print 'Restaurants:'
        for rest in self.filtered_restaurants:
            rest.print_me()

    # Sorts by niceness or distance (easily expandable)
    def sort_output(self, parameter=None):
        for restaurant in self.filtered_restaurants:
            restaurant.compute_fit_rating(self.categories)

        sorted_restaurants = sorted(self.filtered_restaurants, key=lambda restaurant:
                                    restaurant.fit_rating, reverse=True)
        self.filtered_restaurants = sorted_restaurants

        if parameter == 'niceness':
            # Update niceness computation as needed
            for rest in self.filtered_restaurants:
                rest.compute_niceness(self.current_user)

            sorted_restaurants = sorted(self.filtered_restaurants, key=lambda restaurant:
                                        restaurant.niceness, reverse=True)
            self.filtered_restaurants = sorted_restaurants
            self.sorted_by = parameter

        elif parameter == 'distance':
            # First sort by "as the bird flies" distance
            rough_sort = sorted(self.filtered_restaurants, key=lambda restaurant:
                                restaurant.rough_distance)

            count = query_limit

            if len(rough_sort) < query_limit:
                count = len(rough_sort)

            # Query Google to get exact distance
            for index in range(count):
                rough_sort[index].compute_exact_distance(self.current_user)

            # Sort the list again putting the restaurants with exact distances on top
            exact_sort = sorted(rough_sort, key=lambda restaurant:
                                restaurant.exact_distance)

            self.filtered_restaurants = exact_sort
            self.sorted_by = parameter


# This class reads in a TMR, performs various actions and returns
# A dictionary of previous filtering and sorting
# This class can easily be extended to provide extensive recording
# Of past TMRs but such information is not currently utilized
class TMRProcessor:
    restaurant_group = None
    current_user = None

    def __init__(self):
        self.current_user = User(1)
        self.restaurant_group = RestaurantGroup(self.current_user)

    # Processes a TMR... Currently -0 is hardcoded - if TMRs
    # Were properly expanded this could be amended with regex calls
    def process_tmr(self, tmr, queries):
        desired_niceness = None
        desired_type = None
        user_preference = None
        self.restaurant_group.query_current = 0

        # Check for restaurant information query
        if 'RESTAURANT-0' in tmr:
            search_term = []
            name = None
            for value in tmr['RESTAURANT-0']:
                if tmr['RESTAURANT-0'][value] is None:
                    search_term.append(value)
                if value == 'name':
                    name = tmr['RESTAURANT-0'][value]
                #if value == 'genre':
                #    next_place = tmr['RESTAURANT-0'][value]
                #    if next_place[0] == '[':
                #            next_place = next_place[1:-1]
                #
                #    if next_place == 'CATEGORY-0':
                #    if tmr[next_place]:

            if name is not None:
                self.restaurant_group.query_values = search_term
                self.restaurant_group.query_current = 1
                self.restaurant_group.query_name = name
                self.restaurant_group.query_var = None
                self.restaurant_group.narrow('info', None, False)
                return self.restaurant_group.hist_dict()
            else:
                if len(search_term) >= 1:
                    if search_term[0] == 'niceness':
                        desired_niceness = float(tmr['RESTAURANT-0']['niceness'][1:])

        # Check for user's current desire
        if 'MODALITY-0' in tmr:
            if 'scope' in tmr['MODALITY-0']:
                scope = tmr['MODALITY-0']['scope']

                if scope[0] == '[':
                    scope = scope[1:-1]

                if scope in tmr and scope == 'CATEGORY-0':
                    if 'type' in tmr[scope]:
                        desired_type = tmr[scope]['type']
                elif scope in tmr and scope == 'LOOK-FOR-0':
                    if 'theme' in tmr[scope]:
                        theme = tmr[scope]['theme']

                        if theme[0] == '[':
                            theme = theme[1:-1]

                        if theme in tmr and theme == 'RESTAURANT-0':
                            if 'niceness' in tmr[theme]:
                                if tmr[theme]['niceness'][0] == '>' or tmr[theme]['niceness'][0] == '<':
                                    desired_niceness = float(tmr['RESTAURANT-0']['niceness'][1:])
                                else:
                                    desired_niceness = float(tmr['RESTAURANT-0']['niceness'][1:])

        # Check for human taste preference
        if 'HUMAN-0' in tmr:
            if 'taste' in tmr['HUMAN-0']:
                location = tmr['HUMAN-0']['taste']
                if location[0] == '[':
                    location = location[1:-1]

                if location in tmr:
                    if 'type' in tmr[location]:
                        user_preference = tmr[location]['type']

        # Apply history
        for what, value in queries.iteritems():
            try:
                if type(value) != 0:
                    self.restaurant_group.narrow(what, value, False)
            except TypeError:
                pass
        # Apply new niceness desire
        if desired_niceness is not None:
            self.restaurant_group.narrow('niceness', desired_niceness, False)
        # Apply new type desire
        if desired_type is not None:
            self.restaurant_group.narrow('category', desired_type, False)
        # Apply new type desire and store user preference
        if user_preference is not None:
            self.current_user.add_preference(desired_type)
            self.restaurant_group.narrow('category', user_preference, False)
        return self.restaurant_group.hist_dict()


# This function uses the haversine formula to calculate
# distance "as the bird flies" using two lat/long coordinates
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


# This function uses Google to calculate a precise route
# Distance using two lat/long coordinates
def route_distance(lat1, long1, lat2, long2):
    # Form a request string to Google's web API
    request_string = ('http://maps.googleapis.com/maps/api/distancematrix/json?origins='
                      '%f,%f&destinations=%f,%f&mode=driving&sensor=false') % (lat1, long1, lat2, long2)

    # Send the query and convert + store the response
    response = urllib2.urlopen(request_string)
    json_response = response.read()
    dict_response = json.loads(json_response)

    # Interpret the json dict and convert meters to miles
    meters = dict_response['rows'][0]['elements'][0]['distance']['value']
    miles = meters * 0.000621371

    return miles


tmr1 = {"RESTAURANT-0": {"niceness": ">0.75"}, "HUMAN-0": {}, "SPECIFY-TIME-0": {"theme": "LOOK-FOR-0", "time": "TIME-1"}, "LOOK-FOR-0": {"time-of": "TIME-0", "beneficiary": "HUMAN-1", "theme": "RESTAURANT-0", "scope": "EAT-0"}, "HUMAN-1": {"gender": "male"}, "TIME-1": {"dayOfWeek": -1, "hour": 7, "month": -1, "dayOfMonth": -1, "minute": 0, "pm": 1}, "MODALITY-0": {"attribituted-to": "HUMAN-0", "scope": "LOOK-FOR-0", "type": "volitive", "value": 1}, "EAT-0": {"scope": "evening", "location": "RESTAURANT-0"}, "TIME-0": {"dayOfWeek": 7, "hour": -1, "month": 11, "dayOfMonth": 7, "minute": -1, "pm": -1}}

#a = datetime.datetime.now()
#processor = TMRProcessor()
#query_dict = processor.process_tmr(tmr1, {})
#processor.restaurant_group.narrow('niceness', 0.3, False)
#print query_dict
##query_dict = processor.process_tmr(tmr1, query_dict)
##print query_dict
#processor.restaurant_group.print_me()
##print 'NEXT'
#processor.restaurant_group.narrow('category', 'mexican', False)
#processor.restaurant_group.print_me()
#print 'NEXT'
#processor.restaurant_group.narrow('category', 'fast food', False)
#processor.restaurant_group.print_me()
#print 'NEXT'
#print processor.restaurant_group.categories
#query_dict = processor.process_tmr(tmr1, {})
#print processor.restaurant_group.categories
##processor.restaurant_group.print_me()
#print processor.restaurant_group.categories
#query_dict = processor.process_tmr('', query_dict)
#print processor.restaurant_group.categories
#processor.restaurant_group.sort_output('niceness')
#print processor.restaurant_group.categories
##processor.restaurant_group.sort_output('niceness')
#processor.restaurant_group.print_me()
#b = datetime.datetime.now()
#c = b - a
#print c.microseconds
#processor.restaurant_group.print_me()
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

#group_test = RestaurantGroup(User(1))
#group_test.narrow('category', 'mexican', False)
#group_test.narrow('category', 'carry out', False)
#group_test.narrow('niceness', 0.75, False)
#group_test.narrow('category', 'mexican', True)
#group_test.sort_output('niceness')
#group_test.print_me()
