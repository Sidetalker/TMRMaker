# -*- coding: utf-8 -*-
from south.utils import datetime_utils as datetime
from south.db import db
from south.v2 import SchemaMigration
from django.db import models


class Migration(SchemaMigration):

    def forwards(self, orm):
        # Deleting field 'Conversation.question_pending'
        db.delete_column(u'conversations_conversation', 'question_pending')

        # Adding field 'Conversation.pending_question'
        db.add_column(u'conversations_conversation', 'pending_question',
                      self.gf('django.db.models.fields.CharField')(default='', max_length=255),
                      keep_default=False)


    def backwards(self, orm):

        # User chose to not deal with backwards NULL issues for 'Conversation.question_pending'
        raise RuntimeError("Cannot reverse this migration. 'Conversation.question_pending' and its values cannot be restored.")
        
        # The following code is provided here to aid in writing a correct migration        # Adding field 'Conversation.question_pending'
        db.add_column(u'conversations_conversation', 'question_pending',
                      self.gf('django.db.models.fields.CharField')(max_length=255),
                      keep_default=False)

        # Deleting field 'Conversation.pending_question'
        db.delete_column(u'conversations_conversation', 'pending_question')


    models = {
        u'conversations.conversation': {
            'Meta': {'object_name': 'Conversation'},
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'pending_question': ('django.db.models.fields.CharField', [], {'max_length': '255'}),
            'query_dict': ('picklefield.fields.PickledObjectField', [], {}),
            'user_id': ('django.db.models.fields.CharField', [], {'max_length': '255'})
        }
    }

    complete_apps = ['conversations']