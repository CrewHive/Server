
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV PORT=8080
ENV DB_POOL_MAX=16
ENV DB_POOL_MIN=2

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]