FROM eclipse-temurin:17-jre-jammy

ARG JAR_FILE=target/reminder-backend-*.jar

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system reminder \
    && useradd --system --gid reminder --home-dir /app --shell /usr/sbin/nologin reminder

WORKDIR /app

COPY --chown=reminder:reminder ${JAR_FILE} /app/app.jar

USER reminder

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=4 \
    CMD curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health >/dev/null || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
