import uuid
from django.shortcuts import render


def home(request):
    context = {
        'chat_id': uuid.uuid1(),
    }
    return render(request, "chat.html", context)