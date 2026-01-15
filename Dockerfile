#
# Build stage
#
FROM maven:3.9.6-amazoncorretto-21 AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package

#
# Package stage
#
FROM amazoncorretto:21.0.9
COPY --from=build /home/app/target/sgc-discord-bot-*.jar /usr/local/lib/SGC-Discord-Bot.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/usr/local/lib/SGC-Discord-Bot.jar", "-Xmx512m", "-Xms512m", "</dev/null 2>&1 &"] 