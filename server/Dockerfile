# syntax=docker/dockerfile:1
#
# The server image: a JRE plus the distribution `./gradlew :server:installDist` built on the host or
# CI runner. Gradle does not run in here — settings.gradle.kts includes the Android modules, which
# cannot be configured without an Android SDK.
#
#   ./gradlew :server:installDist
#   docker build -t polleninfo-server:local server
#
# No secrets and no test classes: the build context is only build/install/server (.dockerignore).
FROM eclipse-temurin:21.0.12.1_1-jre-resolute@sha256:e9a838377dc3719c0a0e7d1ac2e13d6f49c5cacd643f9a22024845736bb00ad9

# A fixed uid, so secret files on the host can be owned by exactly this user (mode 0400).
ARG APP_UID=10001
RUN groupadd --system --gid "${APP_UID}" polleninfo \
    && useradd --system --uid "${APP_UID}" --gid polleninfo --no-create-home --shell /usr/sbin/nologin polleninfo

COPY --chown=root:root build/install/server /opt/polleninfo-server

# Read by the start script Gradle's application plugin generates. A capped heap leaves the rest of
# the container's memory to metaspace and threads; an OutOfMemoryError ends the process so the
# container restarts instead of limping on.
ENV JAVA_OPTS="-Xmx512m -XX:+ExitOnOutOfMemoryError"

USER ${APP_UID}:${APP_UID}
EXPOSE 8080
ENTRYPOINT ["/opt/polleninfo-server/bin/server"]
