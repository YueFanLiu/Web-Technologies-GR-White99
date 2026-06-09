FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app
COPY . .
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=build /app/target/ProjectWeb-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9192
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
