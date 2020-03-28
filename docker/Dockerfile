FROM azul/zulu-openjdk-alpine:11-jre as builder
COPY . /usr/src/myapp
WORKDIR /usr/src/myapp
RUN ./gradlew --no-daemon  shadowJar

FROM azul/zulu-openjdk-alpine:11-jre
COPY --from=builder /usr/src/myapp/build/libs/bravodb-client-0.1-all.jar /app/bravodb-client.jar
WORKDIR /app
CMD java -jar bravodb-client.jar -Xmx512m
