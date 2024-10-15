package sgc.discord.bot.commands.impl;

import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.slf4j.Logger;

import sgc.bungie.api.processor.RaidReportTool;
import sgc.discord.bot.BotApplication;
import sgc.discord.bot.commands.Command;
import sgc.types.Platform;

public class SGCActivityReportCommand implements Command {

        private static final Logger LOGGER = BotApplication.getLogger();

        @Override
        public void handle(SlashCommandInteraction slashCommandInteraction) {
                System.gc();
                String startDateStr = slashCommandInteraction.getOptionByName("StartDate").get().getStringValue().get();
                String endDateStr = slashCommandInteraction.getOptionByName("EndDate").get().getStringValue().get();

                LOGGER.info(String.format(
                                "Running SGCWeeklyActivityReportCommand (StartDate:%s | EndDate:%s)",
                                startDateStr, endDateStr));

                slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {

                        interactionOriginalResponseUpdater.setContent(
                                        String.format("Building a SGC Activity Report from %s to %s\nThis will take a while.",
                                                        startDateStr, endDateStr))
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
                                        if (durationInDays > 60) {
                                                interactionOriginalResponseUpdater.setContent("")
                                                                .addEmbed(new EmbedBuilder().setTitle(String.format(
                                                                                "SGC Activity Reports from %s to %s",
                                                                                startDate.toString(),
                                                                                endDate.toString()))
                                                                                .setDescription(String.format(
                                                                                                "%d days exceeds the maximum 60 days for SGC Activity Reports",
                                                                                                durationInDays))
                                                                                .setFooter("ERROR")
                                                                                .setThumbnail(getClass()
                                                                                                .getClassLoader()
                                                                                                .getResourceAsStream(
                                                                                                                "thumbnail.jpg"))
                                                                                .setColor(Color.RED))
                                                                .update();
                                        } else {
                                                Instant start = Instant.now();
                                                HashMap<Platform, String> sgcWeeklyActivityReport = RaidReportTool
                                                                .getSGCWeeklyActivityReport(startDate,
                                                                                endDate,
                                                                                interactionOriginalResponseUpdater,
                                                                                slashCommandInteraction.getChannel()
                                                                                                .get(),
                                                                                slashCommandInteraction.getUser());
                                                Instant end = Instant.now();
                                                Duration timeElapsed = Duration.between(start, end);
                                                long timeElapsedInSeconds = timeElapsed.getSeconds();

                                                long hours = timeElapsedInSeconds / 3600;
                                                long minutes = (timeElapsedInSeconds % 3600) / 60;
                                                long seconds = timeElapsedInSeconds % 60;

                                                if (sgcWeeklyActivityReport.isEmpty()) {
                                                        new MessageBuilder().setContent(String.format(
                                                                        "SGC Activity Reports from %s to %s",
                                                                        startDate.toString(),
                                                                        endDate.toString()))
                                                                        .addEmbed(new EmbedBuilder()
                                                                                        .setTitle(String.format(
                                                                                                        "SGC Activity Reports from %s to %s",
                                                                                                        startDate.toString(),
                                                                                                        endDate.toString()))
                                                                                        .setDescription("An Error occured. Please contact Hoppefalcon")
                                                                                        .setFooter("ERROR")
                                                                                        .setThumbnail(getClass()
                                                                                                        .getClassLoader()
                                                                                                        .getResourceAsStream(
                                                                                                                        "thumbnail.jpg"))
                                                                                        .setColor(Color.RED))
                                                                        .send(slashCommandInteraction.getChannel()
                                                                                        .get());
                                                } else {
                                                        LOGGER.info("Sending SGC Activity Reports to "
                                                                        + slashCommandInteraction
                                                                                        .getChannel().get()
                                                                                        .getIdAsString());

                                                        sgcWeeklyActivityReport.forEach((platform, report) -> {
                                                                new MessageBuilder()
                                                                                .setContent(String.format(
                                                                                                "%s Activity Report from %s to %s",
                                                                                                platform,
                                                                                                startDate.toString(),
                                                                                                endDate.toString()))
                                                                                .addEmbed(new EmbedBuilder()
                                                                                                .setAuthor(slashCommandInteraction
                                                                                                                .getUser())
                                                                                                .setTitle(String.format(
                                                                                                                "%s Activity Report from %s to %s",
                                                                                                                platform,
                                                                                                                startDate.toString(),
                                                                                                                endDate.toString()))
                                                                                                .setDescription(String
                                                                                                                .format(
                                                                                                                                "Completed in %02d:%02d:%02d",
                                                                                                                                hours,
                                                                                                                                minutes,
                                                                                                                                seconds))
                                                                                                .setFooter("#AreYouShrouded")
                                                                                                .setThumbnail(getClass()
                                                                                                                .getClassLoader()
                                                                                                                .getResourceAsStream(
                                                                                                                                "thumbnail.jpg"))
                                                                                                .setColor(Color.ORANGE))
                                                                                .addAttachment(report.getBytes(),
                                                                                                String.format("%s_Activity_Report_%s_to_%s.csv",
                                                                                                                platform,
                                                                                                                startDate.toString(),
                                                                                                                endDate.toString()))
                                                                                .send(slashCommandInteraction
                                                                                                .getChannel()
                                                                                                .get());
                                                        });

                                                }
                                        }
                                } else {
                                        interactionOriginalResponseUpdater.setContent(String.format(
                                                        "SGC Activity Reports from %s to %s",
                                                        startDateStr,
                                                        endDateStr))
                                                        .addEmbed(new EmbedBuilder()
                                                                        .setTitle(String.format(
                                                                                        "SGC Activity Reports from %s to %s",
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
                                new MessageBuilder().setContent(
                                                String.format("SGC Activity Reports from %s to %s",
                                                                startDateStr,
                                                                endDateStr))
                                                .addEmbed(new EmbedBuilder()
                                                                .setTitle(String.format(
                                                                                "SGC Activity Reports from %s to %s",
                                                                                startDateStr,
                                                                                endDateStr))
                                                                .setDescription("An Error occured. Please contact Hoppefalcon")
                                                                .setFooter("ERROR")
                                                                .setThumbnail(getClass().getClassLoader()
                                                                                .getResourceAsStream("thumbnail.jpg"))
                                                                .setColor(Color.RED))
                                                .send(slashCommandInteraction.getChannel().get());
                        }
                });
        }

}
