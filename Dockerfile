# syntax=docker/dockerfile:1

# --- Build stage --------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy only the POMs first so Docker can cache the dependency layer
# independently of source code changes.
COPY pom.xml .
COPY riseflow-core/pom.xml riseflow-core/pom.xml
COPY riseflow-module-tenant/pom.xml riseflow-module-tenant/pom.xml
COPY riseflow-module-accounts/pom.xml riseflow-module-accounts/pom.xml
COPY riseflow-module-deals/pom.xml riseflow-module-deals/pom.xml
COPY riseflow-module-marketing/pom.xml riseflow-module-marketing/pom.xml
COPY riseflow-module-commercial/pom.xml riseflow-module-commercial/pom.xml
COPY riseflow-api/pom.xml riseflow-api/pom.xml
RUN mvn -q -B dependency:go-offline

COPY riseflow-core riseflow-core
COPY riseflow-module-tenant riseflow-module-tenant
COPY riseflow-module-accounts riseflow-module-accounts
COPY riseflow-module-deals riseflow-module-deals
COPY riseflow-module-marketing riseflow-module-marketing
COPY riseflow-module-commercial riseflow-module-commercial
COPY riseflow-api riseflow-api
RUN mvn -q -B -pl riseflow-api -am package -DskipTests

# --- Runtime stage -------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system riseflow && useradd --system --gid riseflow riseflow
COPY --from=build /workspace/riseflow-api/target/riseflow-api-*.jar app.jar
RUN chown riseflow:riseflow app.jar
USER riseflow

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
