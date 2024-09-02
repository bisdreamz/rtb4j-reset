FROM maven:3.9.9-eclipse-temurin-21 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and download the dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and build the application
COPY src ./src
ARG MAVEN_BUILD_OPTS="-DskipTests"
RUN mvn clean package ${MAVEN_BUILD_OPTS}
RUN ls -la /app/target

# Use an OpenJDK runtime as the base image for the final stage
FROM openjdk:21

# Set the working directory inside the container
WORKDIR /app

# Copy the built jar file from the previous stage
COPY --from=build /app/target/rtb4j-reset-1.0-SNAPSHOT.jar rtb4j-reset.jar

COPY docker/config.alloy .

# Expose the port your application runs on
EXPOSE 8080

# Run the application
ARG JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java -jar $JAVA_OPTS rtb4j-reset.jar"]
