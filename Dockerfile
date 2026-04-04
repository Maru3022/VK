# Stage 1
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl wget
RUN addgroup -S vk && adduser -S vk -G vk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER vk
EXPOSE 9090 8080
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
