# Build stage
FROM maven:3.9.11-amazoncorretto-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# Run stage
FROM amazoncorretto:17-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S -G app app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
