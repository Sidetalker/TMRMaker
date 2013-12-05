from django.conf.urls import patterns, include, url

urlpatterns = patterns('',
    url(r'^(.+)/$', 'leia.conversations.views.listen', name='listen'),
)
