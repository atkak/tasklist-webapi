FROM java:8-jdk-alpine

RUN apk update
RUN apk --no-cache add bash
RUN apk --no-cache add tzdata \
 && cp /usr/share/zoneinfo/Asia/Tokyo /etc/localtime \
 && apk del tzdata

WORKDIR /opt/tasklist-webapi
COPY target/universal/stage /opt/tasklist-webapi

EXPOSE 9000

ENTRYPOINT ["/opt/tasklist-webapi/bin/tasklist-webapi"]
