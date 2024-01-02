FROM openjdk:17

# Path: /app
WORKDIR /app

COPY ./discovery-server/target/discovery-server-0.0.1-SNAPSHOT.jar /app

EXPOSE 8761

CMD ["java", "-jar", "discovery-server-0.0.1-SNAPSHOT.jar"]


