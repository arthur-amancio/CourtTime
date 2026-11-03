# syntax=docker/dockerfile:1
FROM eclipse-temurin:17-jdk-jammy@sha256:81336a07f69ac361c7f278183b195e32037ebdb532684996806265a3b47ef516 AS build
# O allowlist de .dockerignore inclui somente fontes e arquivos de build.
COPY . /build/
# Secrets opcionais servem somente a redes de desenvolvimento com proxy/CA.
# No Render, o build usa os repositórios públicos e não recebe credenciais da aplicação.
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    --mount=type=secret,id=java_truststore,target=/tmp/build-cacerts \
    --mount=type=secret,id=ca_bundle,target=/tmp/build-ca.pem \
    --mount=type=secret,id=wget_config,target=/root/.wgetrc \
    cd /build && chmod +x mvnw && \
    if [ -f /tmp/build-cacerts ]; then export MAVEN_OPTS=-Djavax.net.ssl.trustStore=/tmp/build-cacerts; fi && \
    if [ -f /tmp/build-ca.pem ]; then export CURL_CA_BUNDLE=/tmp/build-ca.pem; fi && \
    ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:17-jre-jammy@sha256:c35794d1348777e6bc310b9b7f420098ef8f37e988199549b545fded79a895a2
RUN apt-get update && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 courttime \
    && useradd --uid 10001 --gid 10001 --create-home courttime
COPY --from=build /build/target/courttime-0.0.1-SNAPSHOT.jar /app/courttime.jar
ENV SPRING_PROFILES_ACTIVE=render \
    HOME=/home/courttime \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=45.0 -XX:+UseSerialGC -Xss512k -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=32m -XX:MaxDirectMemorySize=16m"
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=180s --retries=3 \
    CMD curl --fail --silent --max-time 4 "http://127.0.0.1:${PORT:-8080}/healthz" || exit 1
ENTRYPOINT ["java", "-jar", "/app/courttime.jar"]
