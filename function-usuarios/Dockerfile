FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY host.json .
COPY src ./src
RUN mvn -B -DskipTests clean package azure-functions:package

FROM mcr.microsoft.com/azure-functions/java:4-java17
ENV AzureWebJobsScriptRoot=/home/site/wwwroot \
    AzureFunctionsJobHost__Logging__Console__IsEnabled=true
COPY --from=build /workspace/target/azure-functions/function-usuarios-local /home/site/wwwroot
EXPOSE 80
