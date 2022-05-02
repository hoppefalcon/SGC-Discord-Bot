package sgc.discord.bot.commands.impl;

import java.awt.Color;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.slf4j.Logger;

import sgc.bungie.api.processor.Member;
import sgc.bungie.api.processor.RaidReportTool;
import sgc.discord.bot.BotApplication;
import sgc.discord.bot.commands.Command;

public class UserCommunityActivityReportCommand implements Command {

        private static final Logger LOGGER = BotApplication.getLogger();

        @Override
        public void handle(SlashCommandInteraction slashCommandInteraction) {
                String userBungieId = slashCommandInteraction.getOptionByName("BungieID").get().getStringValue().get();
                String startDateStr = slashCommandInteraction.getOptionByName("StartDate").get().getStringValue().get();
                String endDateStr = slashCommandInteraction.getOptionByName("EndDate").get().getStringValue().get();

                LOGGER.info(String.format(
                                "Running UserCommunityActivityReportCommand for %s (StartDate:%s | EndDate:%s)",
                                userBungieId, startDateStr, endDateStr));

                slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {

                        interactionOriginalResponseUpdater.setContent(
                                        String.format("Building a SGC activity report from %s to %s for %s",
                                                        startDateStr, endDateStr,
                                                        userBungieId))
                                        .update();

                        try {
                                LocalDate startDate = LocalDate.parse(startDateStr, DateTimeFormatter.BASIC_ISO_DATE);
                                LocalDate endDate = LocalDate.parse(endDateStr, DateTimeFormatter.BASIC_ISO_DATE);

                                Member member = RaidReportTool.getUserCommunityActivityReport(userBungieId, startDate,
                                                endDate,
                                                interactionOriginalResponseUpdater,
                                                slashCommandInteraction.getChannel().get(),
                                                slashCommandInteraction.getUser());

                                if (member == null) {
                                        new MessageBuilder().setContent(String.format(
                                                        "SGC activity report from %s to %s for %s",
                                                        startDate.toString(),
                                                        endDate.toString(),
                                                        userBungieId))
                                                        .addEmbed(new EmbedBuilder()
                                                                        .setTitle(String.format(
                                                                                        "SGC activity report from %s to %s for %s",
                                                                                        startDate.toString(),
                                                                                        endDate.toString(),
                                                                                        userBungieId))
                                                                        .setDescription("An Error occured Finding this Guardian. Please make sure to provide the full BungieID (Guardian#0000)")
                                                                        .setFooter("ERROR")
                                                                        .setThumbnail(getClass().getClassLoader()
                                                                                        .getResourceAsStream(
                                                                                                        "thumbnail.jpg"))
                                                                        .setColor(Color.RED))
                                                        .send(slashCommandInteraction.getChannel().get());
                                } else {
                                        LOGGER.info("Sending SGC Activity Reports to " + slashCommandInteraction
                                                        .getChannel().get().getIdAsString());

                                        new MessageBuilder()
                                                        .setContent(String.format(
                                                                        "SGC Activity Report for %s from %s to %s",
                                                                        member.getCombinedBungieGlobalDisplayName(),
                                                                        startDate.toString(),
                                                                        endDate.toString()))
                                                        .addEmbed(new EmbedBuilder()
                                                                        .setAuthor(slashCommandInteraction
                                                                                        .getUser())
                                                                        .setTitle(String.format(
                                                                                        "SGC Activity Report for %s from %s to %s",
                                                                                        member.getCombinedBungieGlobalDisplayName(),
                                                                                        startDate.toString(),
                                                                                        endDate.toString()))
                                                                        .setDescription(String.format(
                                                                                        "Community Activity Points: ",
                                                                                        member.getWeeklySGCActivity()
                                                                                                        .get("SCORE")))
                                                                        .setFooter("#AreYouShrouded")
                                                                        .setThumbnail(getClass()
                                                                                        .getClassLoader()
                                                                                        .getResourceAsStream(
                                                                                                        "thumbnail.jpg"))
                                                                        .setColor(Color.ORANGE))
                                                        .send(slashCommandInteraction.getChannel().get());
                                }
                        } catch (Exception e) {
                                LOGGER.error(e.getMessage(), e);
                                new MessageBuilder().setContent(String.format(
                                                "SGC activity report from %s to %s for %s",
                                                startDateStr,
                                                endDateStr,
                                                userBungieId))
                                                .addEmbed(new EmbedBuilder()
                                                                .setTitle(String.format(
                                                                                "SGC activity report from %s to %s for %s",
                                                                                startDateStr,
                                                                                endDateStr,
                                                                                userBungieId))
                                                                .setDescription("An Error occured Finding this Guardian. Please make sure to provide the full BungieID (Guardian#0000)")
                                                                .setFooter("ERROR")
                                                                .setThumbnail(getClass().getClassLoader()
                                                                                .getResourceAsStream(
                                                                                                "thumbnail.jpg"))
                                                                .setColor(Color.RED))
                                                .send(slashCommandInteraction.getChannel().get());
                        }
                });
        }

}
