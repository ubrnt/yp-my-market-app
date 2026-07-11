FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./
COPY shop/build.gradle shop/build.gradle
COPY payment-service/build.gradle payment-service/build.gradle
RUN ./gradlew --no-daemon :payment-service:resolveDependencies
COPY openapi/ openapi/
COPY payment-service/src payment-service/src
RUN ./gradlew --no-daemon -x test :payment-service:bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/payment-service/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
