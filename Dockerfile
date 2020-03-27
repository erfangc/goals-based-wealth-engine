FROM openjdk:11-slim

ADD target/wealth-engine.jar /app/wealth-engine.jar
ADD bin /app/bin

EXPOSE 8080

ENTRYPOINT ["java", "-jar","/app/wealth-engine.jar", "-Djava.library.path", "/app/bin"]
