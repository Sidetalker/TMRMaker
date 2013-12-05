from django.conf.urls import patterns, include, url

from django.contrib import admin
admin.autodiscover()

urlpatterns = patterns('',
    url(r'^$', 'leia.interface.views.home', name='home'),
    url(r'^account/', include('leia.accounts.urls')),
    url(r'^converse/', include('leia.conversations.urls')),
    url(r'^admin/', include(admin.site.urls)),

    url(r'^admin/', include(admin.site.urls)),
)
