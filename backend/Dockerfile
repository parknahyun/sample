# `full` 프로필용 멀티 스테이지 빌드
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test -x spotlessCheck -x checkstyleMain -x checkstyleTest

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home appuser && mkdir -p /data/images && chown -R appuser /data
COPY --from=build /workspace/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
