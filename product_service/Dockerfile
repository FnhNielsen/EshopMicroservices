FROM openjdk:21

# RUN mvn clean package
# Setting up work directory
WORKDIR /app

# Copy the jar file into our app
COPY ./product_service/target/product_service-0.0.1-SNAPSHOT.jar /app

# Exposing port 8080
EXPOSE 8080

# Starting the application
CMD ["java", "-jar", "product_service-0.0.1-SNAPSHOT.jar"]
