FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -B -ntp verify

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --gid 10001 chatbot && useradd --uid 10001 --gid chatbot --no-create-home chatbot \
    && mkdir /data && chown chatbot:chatbot /data
WORKDIR /app
COPY --from=build /build/target/portfolio-chat.jar /app/chat.jar
ENV BUDGET_DB=/data/budget.db
USER chatbot
EXPOSE 8080
ENTRYPOINT ["java", "-Djdk.httpclient.disableRetryConnect=true", "-XX:MaxRAMPercentage=70", "-jar", "/app/chat.jar"]
