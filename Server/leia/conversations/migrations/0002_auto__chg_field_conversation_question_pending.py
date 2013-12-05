# -*- coding: utf-8 -*-
from south.utils import datetime_utils as datetime
from south.db import db
from south.v2 import SchemaMigration
from django.db import models


class Migration(SchemaMigration):

    def forwards(self, orm):

        # Changing field 'Conversation.question_pending'
        db.alter_column(u'conversations_conversation', 'question_pending', self.gf('django.db.models.fields.NullBooleanField')(null=True))

    def backwards(self, orm):

        # User chose to not deal with backwards NULL issues for 'Conversation.question_pending'
        raise RuntimeError("Cannot reverse this migration. 'Conversation.question_pending' and its values cannot be restored.")
        
        # The following code is provided here to aid in writing a correct migration
        # Changing field 'Conversation.question_pending'
        db.alter_column(u'conversations_conversation', 'question_pending', self.gf('django.db.models.fields.BooleanField')())

    models = {
        u'conversations.conversation': {
            'Meta': {'object_name': 'Conversation'},
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'query_dict': ('picklefield.fields.PickledObjectField', [], {}),
            'question_pending': ('django.db.models.fields.NullBooleanField', [], {'null': 'True', 'blank': 'True'}),
            'user_id': ('django.db.models.fields.CharField', [], {'max_length': '255'})
        }
    }

    complete_apps = ['conversations']