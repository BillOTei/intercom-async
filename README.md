An http event based broker service with optional rabbitmq integration 
for events, written in scala. All main Intercom api endpoints
are integrated as final event receiver example. Sentry logback integration
is also available.

**Contact endpoints:**

    POST /contact/user 
        see /doc/contact
        
    POST /contact/lead 
        see /doc/contact

**Event endpoints**
    
    POST /event/add
        see /doc/event 
        
Some business specific events are already implemented such as
user creation/update, company creation/update... See [ForwardActor.scala](https://github.com/BillOTei/intercom-async/blob/develop/app/service/actors/ForwardActor.scala)
for jsons formats details.
