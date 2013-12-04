import os
import json
import pickle
import subprocess

from django.http import HttpResponse
from django.conf import settings
from django.views.decorators.csrf import csrf_exempt

from leia.conversations.models import Conversation


@csrf_exempt
def listen(request, conversation_user_id):
    sentence = request.POST.get("sentence")
    try:
        conversation = Conversation.objects.get(user_id=conversation_user_id)
    except:
        conversation = Conversation()
        conversation.user_id = conversation_user_id
        conversation.query_dict = {}

    environment = os.environ.copy()

    environment['PATH'] = settings.LEIA_SRC_DIR + ":" + environment['PATH']
    environment['CLASSPATH'] = os.path.join(settings.LEIA_SRC_DIR, 'sqlite-jdbc-3.7.2.jar')
    environment['CLASSPATH'] += ":" + os.path.join(settings.LEIA_SRC_DIR, 'jyson-1.0.2.jar')

    leia = subprocess.Popen(['jython', '../Communicator/leia.py', str(sentence), str(conversation.pending_question)],
                            stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                            env=environment, cwd=settings.LEIA_WORKING_DIR)

    query_dict = pickle.dumps(conversation.query_dict)
    result = pickle.loads(leia.communicate(input=query_dict)[0])

    if result.get('debug'):
        return HttpResponse(result['debug'])

    # Keep track of the current topic
    question = result.get('question')
    conversation.pending_question = question

    answered_questions = result.get('answered_questions')
    conversation.answered_questions = answered_questions

    conversation.query_dict = result['query_dict']
    conversation.save()

    return HttpResponse(json.dumps(result))