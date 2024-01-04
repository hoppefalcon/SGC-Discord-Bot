package sgc.discord.bot.commands.impl;

import java.awt.Color;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.slf4j.Logger;

import sgc.bungie.api.processor.RaidReportTool;
import sgc.discord.bot.BotApplication;
import sgc.discord.bot.commands.Command;

public class UserWeeklyRaidReportCommand implements Command {

        private static final Logger LOGGER = BotApplication.getLogger();

        @Override
        public void handle(SlashCommandInteraction slashCommandInteraction) {
                System.gc();
                String userBungieId = slashCommandInteraction.getOptionByName("BungieID").get().getStringValue().get();
                String startDateStr = slashCommandInteraction.getOptionByName("StartDate").get().getStringValue().get();
                String endDateStr = slashCommandInteraction.getOptionByName("EndDate").get().getStringValue().get();

                LOGGER.info(String.format(
                                "Running UserWeeklyRaidReportCommand (BungieID:%s | StartDate:%s | EndDate:%s)",
                                userBungieId, startDateStr, endDateStr));

                slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {

                        interactionOriginalResponseUpdater.setContent(String
                                        .format("Building a weekly raid report from %s to %s for %s", startDateStr,
                                                        endDateStr, userBungieId))
                                        .update();

                        try {
                                if (RaidReportTool.isValidDateFormat(startDateStr)
                                                && RaidReportTool.isValidDateFormat(endDateStr)) {
                                        LocalDate startDate = LocalDate.parse(startDateStr,
                                                        DateTimeFormatter.BASIC_ISO_DATE);
                                        LocalDate endDate = LocalDate.parse(endDateStr,
                                                        DateTimeFormatter.BASIC_ISO_DATE);
                                        Period period = Period.between(startDate, endDate);
                                        long durationInDays = period.getDays() + (period.getMonths() * (365 / 12))
                                                        + (period.getYears() * 365);
                                        if (durationInDays > 7) {
                                                interactionOriginalResponseUpdater.setContent("")
                                                                .addEmbed(new EmbedBuilder()
                                                                                .setTitle(String.format(
                                                                                                "%s Raid Report from %s to %s",
                                                                                                userBungieId,
                                                                                                startDate.toString(),
                                                                                                endDate.toString()))
                                                                                .setDescription(String.format(
                                                                                                "%d days exceeds the maximum 7 days for User Weekly Raid Reports",
                                                                                                durationInDays))
                                                                                .setFooter("ERROR")
                                                                                .setThumbnail(getClass()
                                                                                                .getClassLoader()
                                                                                                .getResourceAsStream(
                                                                                                                "thumbnail.jpg"))
                                                                                .setColor(Color.RED))
                                                                .update();
                                        } else {
                                                String userReport = RaidReportTool.getUserWeeklyClears(userBungieId,
                                                                startDate,
                                                                endDate);
                                                if (userReport.isEmpty()) {
                                                        interactionOriginalResponseUpdater.setContent("")
                                                                        .addEmbed(new EmbedBuilder()
                                                                                        .setTitle(String.format(
                                                                                                        "%s Raid Report from %s to %s",
                                                                                                        userBungieId,
                                                                                                        startDate.toString(),
                                                                                                        endDate.toString()))
                                                                                        .setDescription(
                                                                                                        "An Error occured Finding this Guardian. Please make sure to provide the full BungieID (Guardian#0000)")
                                                                                        .setFooter("ERROR")
                                                                                        .setThumbnail(getClass()
                                                                                                        .getClassLoader()
                                                                                                        .getResourceAsStream(
                                                                                                                        "thumbnail.jpg"))
                                                                                        .setColor(Color.RED))
                                                                        .update();
                                                } else {
                                                        interactionOriginalResponseUpdater.addEmbed(new EmbedBuilder()
                                                                        .setTitle(String.format(
                                                                                        "%s Raid Report from %s to %s",
                                                                                        userBungieId,
                                                                                        startDate.toString(),
                                                                                        endDate.toString()))
                                                                        .setDescription(userReport)
                                                                        .setFooter("Happy Raiding!")
                                                                        .setThumbnail(getClass().getClassLoader()
                                                                                        .getResourceAsStream(
                                                                                                        "thumbnail.jpg"))
                                                                        .setColor(Color.GREEN)).update();
                                                }
                                        }
                                } else {
                                        interactionOriginalResponseUpdater.setContent(String.format(
                                                        "%s Raid Report from %s to %s",
                                                        userBungieId,
                                                        startDateStr,
                                                        endDateStr))
                                                        .addEmbed(new EmbedBuilder()
                                                                        .setTitle(String.format(
                                                                                        "%s Raid Report from %s to %s",
                                                                                        userBungieId,
                                                                                        startDateStr,
                                                                                        endDateStr))
                                                                        .setDescription("The Start and End Dates must be formated as YYYYMMDD.")
                                                                        .setFooter("ERROR")
                                                                        .setThumbnail(getClass()
                                                                                        .getClassLoader()
                                                                                        .getResourceAsStream(
                                                                                                        "thumbnail.jpg"))
                                                                        .setColor(Color.RED))
                                                        .update();
                                }
                        } catch (Exception e) {
                                LOGGER.error(e.getMessage(), e);
                                interactionOriginalResponseUpdater.setContent("").addEmbed(new EmbedBuilder()
                                                .setTitle(String.format("%s Raid Report from %s to %s", userBungieId,
                                                                startDateStr, endDateStr))
                                                .setDescription(
                                                                "An Error occured Finding this Guardian. Please make sure to provide the full BungieID (Guardian#0000)")
                                                .setFooter("ERROR")
                                                .setThumbnail(getClass().getClassLoader()
                                                                .getResourceAsStream("thumbnail.jpg"))
                                                .setColor(Color.RED)).update();
                        }
                });
        }

}
