
# Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# Run
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV SERVER_PORT=8080
EXPOSE 8080
COPY --from=build /app/target/astrokiddo-0.1.0.jar app.jar
ENTRYPOINT ["sh","-c","java -jar /app/app.jar"]
