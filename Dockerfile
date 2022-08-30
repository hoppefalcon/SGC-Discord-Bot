#
# Build stage
#
FROM maven:3.8.6-openjdk-18-slim AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package

#
# Package stage
#
FROM openjdk:18-slim
COPY --from=build /home/app/target/sgc-discord-bot-*.jar /usr/local/lib/SGC-Discord-Bot.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/usr/local/lib/SGC-Discord-Bot.jar","</dev/null 2>&1 &"] 