# ─────────────────────────────────────────────────────────────
# Stage 1 : Build du backend Spring Boot
# Les templates Thymeleaf et ressources statiques (PWA) sont dans src/main/resources
# et sont inclus automatiquement dans le JAR par Maven.
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:25.0.5_8-jdk AS backend-build
WORKDIR /app

ARG GIT_COMMIT=unknown

COPY .mvn .mvn
# Copy Maven wrapper & POM first for dependency caching
COPY mvnw mvnw.cmd pom.xml ./


# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy sources and build
COPY src ./src
RUN ./mvnw package -DskipTests -B -Dgit.commit=${GIT_COMMIT}

# ─────────────────────────────────────────────────────────────
# Stage 2 : Image finale légère (JRE seulement)
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:25.0.5_8-jre
WORKDIR /app



COPY --from=backend-build /app/target/ludalex-*.jar app.jar

RUN groupadd --system appgroup && useradd --system --gid appgroup --no-create-home appuser \
    && mkdir -p /data/images && chown -R appuser:appgroup /data
USER appuser

EXPOSE 8181

ENV APP_TIMEZONE=Europe/Paris
# Raspberry Pi : tas borné, GC série, mode headless (décodage d'image sans écran)
ENV JAVA_OPTS="-Xmx384m -XX:+UseSerialGC -Djava.awt.headless=true"
ENV LUDALEX_IMAGES_REPERTOIRE=/data/images

VOLUME /data/images

# Profil prod → PostgreSQL + Cloudflare Access
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Duser.timezone=Europe/Paris -Dspring.profiles.active=prod -jar app.jar"]