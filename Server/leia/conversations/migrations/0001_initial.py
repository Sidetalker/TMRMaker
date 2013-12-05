# -*- coding: utf-8 -*-
from south.utils import datetime_utils as datetime
from south.db import db
from south.v2 import SchemaMigration
from django.db import models


class Migration(SchemaMigration):

    def forwards(self, orm):
        # Adding model 'Conversation'
        db.create_table(u'conversations_conversation', (
            (u'id', self.gf('django.db.models.fields.AutoField')(primary_key=True)),
            ('user_id', self.gf('django.db.models.fields.CharField')(max_length=255)),
            ('query_dict', self.gf('picklefield.fields.PickledObjectField')()),
            ('question_pending', self.gf('django.db.models.fields.BooleanField')()),
        ))
        db.send_create_signal(u'conversations', ['Conversation'])


    def backwards(self, orm):
        # Deleting model 'Conversation'
        db.delete_table(u'conversations_conversation')


    models = {
        u'conversations.conversation': {
            'Meta': {'object_name': 'Conversation'},
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'query_dict': ('picklefield.fields.PickledObjectField', [], {}),
            'question_pending': ('django.db.models.fields.BooleanField', [], {}),
            'user_id': ('django.db.models.fields.CharField', [], {'max_length': '255'})
        }
    }

    complete_apps = ['conversations']