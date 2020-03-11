FROM openjdk:8-jdk-alpine

RUN addgroup -S spring && adduser -S spring -G spring

USER spring:spring

ADD target/wealth-engine-0.0.1-SNAPSHOT.jar /app/wealth-engine-0.0.1-SNAPSHOT.jar
ADD bin /app/bin

RUN ls /app/bin

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/wealth-engine-0.0.1-SNAPSHOT.jar","-Djava.library.path","/app/bin"]
