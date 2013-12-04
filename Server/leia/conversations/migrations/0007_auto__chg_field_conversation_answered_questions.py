# -*- coding: utf-8 -*-
from south.utils import datetime_utils as datetime
from south.db import db
from south.v2 import SchemaMigration
from django.db import models


class Migration(SchemaMigration):

    def forwards(self, orm):

        # Changing field 'Conversation.answered_questions'
        db.alter_column(u'conversations_conversation', 'answered_questions', self.gf('picklefield.fields.PickledObjectField')(null=True))

    def backwards(self, orm):

        # User chose to not deal with backwards NULL issues for 'Conversation.answered_questions'
        raise RuntimeError("Cannot reverse this migration. 'Conversation.answered_questions' and its values cannot be restored.")
        
        # The following code is provided here to aid in writing a correct migration
        # Changing field 'Conversation.answered_questions'
        db.alter_column(u'conversations_conversation', 'answered_questions', self.gf('picklefield.fields.PickledObjectField')())

    models = {
        u'conversations.conversation': {
            'Meta': {'object_name': 'Conversation'},
            'answered_questions': ('picklefield.fields.PickledObjectField', [], {'null': 'True'}),
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'pending_question': ('django.db.models.fields.CharField', [], {'max_length': '255', 'null': 'True'}),
            'query_dict': ('picklefield.fields.PickledObjectField', [], {}),
            'user_id': ('django.db.models.fields.CharField', [], {'max_length': '255'})
        }
    }

    complete_apps = ['conversations']