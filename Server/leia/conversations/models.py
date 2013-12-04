from django.db import models
from picklefield.fields import PickledObjectField
from django.contrib import admin

class Conversation(models.Model):
    user_id = models.CharField(max_length=255)
    query_dict = PickledObjectField()
    answered_questions = PickledObjectField(null=True)
    pending_question = models.CharField(max_length=255, null=True)

    def __unicode__(self):
        return self.user_id

admin.site.register(Conversation)