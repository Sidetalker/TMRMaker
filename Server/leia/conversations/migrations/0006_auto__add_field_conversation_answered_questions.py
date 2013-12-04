# -*- coding: utf-8 -*-
from south.utils import datetime_utils as datetime
from south.db import db
from south.v2 import SchemaMigration
from django.db import models


class Migration(SchemaMigration):

    def forwards(self, orm):
        # Adding field 'Conversation.answered_questions'
        db.add_column(u'conversations_conversation', 'answered_questions',
                      self.gf('picklefield.fields.PickledObjectField')(default={}),
                      keep_default=False)


    def backwards(self, orm):
        # Deleting field 'Conversation.answered_questions'
        db.delete_column(u'conversations_conversation', 'answered_questions')


    models = {
        u'conversations.conversation': {
            'Meta': {'object_name': 'Conversation'},
            'answered_questions': ('picklefield.fields.PickledObjectField', [], {}),
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'pending_question': ('django.db.models.fields.CharField', [], {'max_length': '255', 'null': 'True'}),
            'query_dict': ('picklefield.fields.PickledObjectField', [], {}),
            'user_id': ('django.db.models.fields.CharField', [], {'max_length': '255'})
        }
    }

    complete_apps = ['conversations']