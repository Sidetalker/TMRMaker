import sys
sys.path.append("../out/artifacts/TMRMaker_jar/TMRMaker.jar")
sys.path.append("../out/production/TMRMaker/parser.jar")

from knowledgeBase.semanticDeriver import Deriver
from leia.parse import DependencyParse
from output import Processor
import pickle

from Restaurants import TMRProcessor
from outputgen import convey

showAllTMRS = False

sentence = sys.argv[1]
pending_question = sys.argv[2]
answered_questions = {} # pickle.loads(sys.argv[3])

if answered_questions == None:
    answered_questions = {}

if sentence == 'init':
    print pickle.dumps({
        'message': convey('offer_help'),
        'query_dict': {},
        'suggestions': [],
    })
    exit()

pickled_query = sys.stdin.read()
query_dict = pickle.loads(pickled_query)

deriver = Deriver()
deriver.addTheorems("ruleList")
deriver.addOntology("ontology.json")
part = DependencyParse.parse(sentence)[0]

deriver.deriveSemantics(part)
interpretationList = deriver.generateTMRInterpretations()

if not interpretationList:
    print pickle.dumps({
        'message': convey('request_additional_info'),
        'query_dict': query_dict,
        'suggestions': [],
        'answered_questions': answered_questions,
    })
    exit()


tmrList = []
bestTMRIndex = -1
bestNum = -1

while interpretationList.hasNext():
    if showAllTMRS:
        print '=' * 68
    numTMRAssignmentsUsed = deriver.assembleTMRs(interpretationList.next())
    if numTMRAssignmentsUsed > bestNum:
        bestTMRIndex = len(tmrList)
        bestNum = numTMRAssignmentsUsed
    tmrList.append(deriver.tmrs)
    if showAllTMRS:
        print numTMRAssignmentsUsed
        deriver.outputTMRs()
    deriver.resetTMRs()

python_tmr = {}

for item in tmrList[bestTMRIndex].values():
    item_name = str(item)
    python_tmr[item_name] = {}
    for detail in item.properties.keys():
        try:
            detail_value = int(item.properties[detail])
        except (ValueError, TypeError):
            detail_value = str(item.properties[detail]).strip("\"'")

        python_tmr[item_name][detail] = detail_value

# print pickle.dumps({
#     'debug': str(python_tmr)
# })
# exit()

processor = TMRProcessor()
query_dict = processor.process_tmr(python_tmr, query_dict)

sort_decided = False

if pending_question == 'importance_of_travel_time':
    if python_tmr.get('BINARY-RESPONSE-0') == None:
        print pickle.dumps({
            'message': "%s %s" % (convey('failure_to_answer'), convey('importance_of_travel_time', "nicencess")),
            'query_dict': query_dict,
            'suggestions': [],
            'question': 'importance_of_travel_time',
            'answered_questions': answered_questions,
        })
        exit()
    elif python_tmr.get('BINARY-RESPONSE-0')['value'] == "true":
        processor.restaurant_group.sort_output('distance')
        answered_questions['importance_of_travel_time'] = "true"
        sort_decided = True
    elif python_tmr.get('BINARY-RESPONSE-0')['value'] == "false":
        processor.restaurant_group.sort_output('niceness')
        sort_decided = True

if query_dict.get('niceness') and sort_decided == False:
    print pickle.dumps({
        'message': convey('importance_of_travel_time', "niceness"),
        'query_dict': query_dict,
        'suggestions': [],
        'question': 'importance_of_travel_time',
        'answered_questions': answered_questions,
    })
    exit()

restaurants = processor.restaurant_group.filtered_restaurants
results_list = []
for restaurant in restaurants:
    results_list.append({
        'id': restaurant.id,
        'name': restaurant.name,
        'rating': restaurant.rating,
        'phone': restaurant.phone,
        'is_open': restaurant.is_open,
        'location': str(restaurant.location),
        'categories': restaurant.categories,
        'niceness': restaurant.niceness,
    })

if len(results_list):
    text_response = convey('suggestion', results_list[0]['name'])

print pickle.dumps({
    'message': text_response,
    'query_dict': query_dict,
    'suggestions': results_list,
    'answered_questions': answered_questions,
    'tmr': python_tmr,
})