package sgc.raid.report.bot.commands.impl;

import java.awt.Color;

import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.slf4j.Logger;

import sgc.raid.report.bot.BotApplication;
import sgc.raid.report.bot.commands.Command;
import sgc.sherpa.sheets.RaidCarnageReport;
import sgc.sherpa.sheets.RaidReportTool;

public class RaidCarnageReportCommand implements Command {
        private static final Logger LOGGER = BotApplication.getLogger();

        @Override
        public void handle(SlashCommandInteraction slashCommandInteraction) {
                String carnageReportID = slashCommandInteraction.getOptionByName("ID").get().getStringValue().get();
                LOGGER.info("Running RaidCarnageReportCommand with option " + carnageReportID);

                slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {

                        interactionOriginalResponseUpdater
                                        .setContent("Building a raid carnage report for " + carnageReportID).update();
                        try {
                                RaidCarnageReport raidCarnageReport = RaidReportTool
                                                .getRaidCarnageReport(carnageReportID);
                                new MessageBuilder().setContent("Raid Postgame Carnage Report")
                                                .addEmbed(new EmbedBuilder()
                                                                .setAuthor(slashCommandInteraction.getUser())
                                                                .setTitle("Raid Postgame Carnage Report")
                                                                .setDescription("Completed")
                                                                .setThumbnail(getClass().getClassLoader()
                                                                                .getResourceAsStream("thumbnail.jpg"))
                                                                .setColor(Color.GREEN))
                                                .addAttachment(raidCarnageReport.getCSV().getBytes(),
                                                                String.format("%s - %s - %s.csv", carnageReportID,
                                                                                raidCarnageReport.getRaid().getName(),
                                                                                raidCarnageReport.getDateCompleted()
                                                                                                .toString()))
                                                .send(slashCommandInteraction.getChannel().get());

                        } catch (Exception e) {
                                LOGGER.error(e.getMessage(), e);
                                interactionOriginalResponseUpdater.setContent("").addEmbed(new EmbedBuilder()
                                                .setTitle("Raid Postgame Carnage Report")
                                                .setDescription("An error occured while building a Raid Postgame Carnage Report for "
                                                                + carnageReportID)
                                                .setFooter("ERROR")
                                                .setThumbnail(getClass().getClassLoader()
                                                                .getResourceAsStream("thumbnail.jpg"))
                                                .setColor(Color.RED)).update();
                        }
                });
        }

}
