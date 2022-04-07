package sgc.raid.report.bot.commands.impl;

import java.awt.Color;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.slf4j.Logger;

import sgc.raid.report.bot.BotApplication;
import sgc.raid.report.bot.commands.Command;
import sgc.sherpa.sheets.RaidReportTool;

public class SGCWeeklyActivityReportCommand implements Command {

        private static final Logger LOGGER = BotApplication.getLogger();

        @Override
        public void handle(SlashCommandInteraction slashCommandInteraction) {
                String startDateStr = slashCommandInteraction.getOptionByName("StartDate").get().getStringValue().get();
                String endDateStr = slashCommandInteraction.getOptionByName("EndDate").get().getStringValue().get();

                LOGGER.info(String.format(
                                "Running SGCWeeklyActivityReportCommand (StartDate:%s | EndDate:%s)",
                                startDateStr, endDateStr));

                slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {

                        interactionOriginalResponseUpdater.setContent(String
                                        .format("Building a SGC weekly activity report from %s to %s", startDateStr,
                                                        endDateStr))
                                        .update();

                        try {
                                LocalDate startDate = LocalDate.parse(startDateStr, DateTimeFormatter.BASIC_ISO_DATE);
                                LocalDate endDate = LocalDate.parse(endDateStr, DateTimeFormatter.BASIC_ISO_DATE);
                                String sgcWeeklyActivityReport = RaidReportTool.getSGCWeeklyActivityReport(startDate,
                                                endDate);

                                if (sgcWeeklyActivityReport.isEmpty()) {
                                        interactionOriginalResponseUpdater.setContent("").addEmbed(new EmbedBuilder()
                                                        .setTitle(String.format(
                                                                        "SGC Weekly Activity Report from %s to %s",
                                                                        startDate.toString(), endDate.toString()))
                                                        .setDescription("An Error occured. Please contact Hoppefalcon")
                                                        .setFooter("ERROR")
                                                        .setThumbnail(getClass().getClassLoader()
                                                                        .getResourceAsStream("thumbnail.jpg"))
                                                        .setColor(Color.RED)).update();
                                } else {
                                        LOGGER.info("Sending SGC Weekly Activity Report to " + slashCommandInteraction
                                                        .getChannel().get().getIdAsString());

                                        new MessageBuilder()
                                                        .addEmbed(new EmbedBuilder()
                                                                        .setAuthor(slashCommandInteraction.getUser())
                                                                        .setTitle(String.format(
                                                                                        "SGC Weekly Activity Report from %s to %s",
                                                                                        startDate.toString(),
                                                                                        endDate.toString()))
                                                                        .setDescription("SGC Weekly Activity Report Completed")
                                                                        .setFooter("#AreYouShrouded")
                                                                        .setThumbnail(getClass().getClassLoader()
                                                                                        .getResourceAsStream(
                                                                                                        "thumbnail.jpg"))
                                                                        .setColor(Color.PINK))
                                                        .addAttachment(sgcWeeklyActivityReport.getBytes(),
                                                                        String.format("SGC_Weekly_Activity_Report [%s-%s].csv",
                                                                                        startDate.toString(),
                                                                                        endDate.toString()))
                                                        .send(slashCommandInteraction.getChannel().get());
                                }
                        } catch (Exception e) {
                                LOGGER.error(e.getMessage(), e);
                                interactionOriginalResponseUpdater.setContent("").addEmbed(new EmbedBuilder()
                                                .setTitle(String.format("SGC Weekly Activity Report from %s to %s",
                                                                startDateStr, endDateStr))
                                                .setDescription("An Error occured. Please contact Hoppefalcon")
                                                .setFooter("ERROR")
                                                .setThumbnail(getClass().getClassLoader()
                                                                .getResourceAsStream("thumbnail.jpg"))
                                                .setColor(Color.RED)).update();
                        }
                });
        }

}
