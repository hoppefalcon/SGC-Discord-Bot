FROM openjdk:slim
VOLUME /tmp
COPY ${JAR_FILE} SGC-Discord-Bot.jar
ENTRYPOINT ["java","-jar","/SGC-Discord-Bot.jar"]