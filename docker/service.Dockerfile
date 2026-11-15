# One image definition for every service. Build the jars first (mvn package), then:
#   docker build -f docker/service.Dockerfile --build-arg MODULE=services/report-service .
# Uses Spring Boot's layered jar so dependency layers are cached between builds.

FROM eclipse-temurin:21-jre-jammy AS extract
ARG MODULE
WORKDIR /build
COPY ${MODULE}/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:21-jre-jammy
# fontconfig: JasperReports needs AWT font metrics for PDF/XLSX export
RUN apt-get update \
    && apt-get install -y --no-install-recommends fontconfig curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app --home /app app
WORKDIR /app
COPY --from=extract /build/extracted/dependencies/ ./
COPY --from=extract /build/extracted/spring-boot-loader/ ./
COPY --from=extract /build/extracted/snapshot-dependencies/ ./
COPY --from=extract /build/extracted/application/ ./
# Pre-create volume mount points so Docker initialises named volumes with the app user as owner.
RUN mkdir -p /app/data/report-storage && chown -R app:app /app
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.awt.headless=true -Duser.timezone=UTC -Dfile.encoding=UTF-8"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
