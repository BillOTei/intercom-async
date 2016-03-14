FROM java:8

MAINTAINER ateilhet@gmail.com

RUN apt-get update && \
    apt-get install unzip -y
    
ADD build.zip /tmp/
RUN unzip -o /tmp/build.zip -d /srv/ && rm /tmp/build.zip

ENTRYPOINT ["/srv/centralapp-events-1.0-SNAPSHOT/bin/centralapp-events"]
WORKDIR '/srv/centralapp-events-1.0-SNAPSHOT/bin'
EXPOSE 9000
EXPOSE 2554
