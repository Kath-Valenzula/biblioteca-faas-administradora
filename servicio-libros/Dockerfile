FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/servicio-libros-1.0.0.jar app.jar
COPY --from=build /workspace/src/main/resources/wallet /app/wallet
EXPOSE 8083
ENTRYPOINT ["java", "-Doracle.net.tns_admin=/app/wallet", "-jar", "/app/app.jar"]
