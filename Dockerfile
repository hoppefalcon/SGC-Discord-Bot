FROM openjdk:slim
ADD /target/sgc-discord-bot-*.jar SGC-Discord-Bot.jar
ENTRYPOINT ["java","-jar","/SGC-Discord-Bot.jar"]