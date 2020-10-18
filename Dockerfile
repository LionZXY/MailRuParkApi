FROM openjdk:8-jdk-alpine3.9 AS builder
WORKDIR /build/
COPY . .
RUN ./gradlew jar

FROM openjdk:8-jre-alpine3.9
RUN apk add --no-cache fontconfig ttf-dejavu
WORKDIR /app/
COPY --from=builder  /build/build/libs/MailRuParkApi.jar .
CMD java -jar MailRuParkApi.jar


