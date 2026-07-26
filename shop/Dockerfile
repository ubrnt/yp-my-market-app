FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./
COPY shop/build.gradle shop/build.gradle
COPY payment-service/build.gradle payment-service/build.gradle
RUN ./gradlew --no-daemon :shop:resolveDependencies
COPY openapi/ openapi/
COPY shop/src shop/src
RUN ./gradlew --no-daemon -x test :shop:bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/shop/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
