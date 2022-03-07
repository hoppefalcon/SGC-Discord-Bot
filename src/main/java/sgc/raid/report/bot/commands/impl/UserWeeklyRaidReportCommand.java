package sgc.raid.report.bot.commands.impl;

import java.awt.Color;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.slf4j.Logger;

import sgc.raid.report.bot.BotApplication;
import sgc.raid.report.bot.commands.Command;
import sgc.sherpa.sheets.RaidReportTool;

public class UserWeeklyRaidReportCommand implements Command {

    private static final Logger LOGGER = BotApplication.getLogger();

    @Override
    public void handle(SlashCommandInteraction slashCommandInteraction) {
        String bungieId = slashCommandInteraction.getOptionByName("BungieID").get().getStringValue().get();
        String startDateStr = slashCommandInteraction.getOptionByName("StartDate").get().getStringValue().get();
        String endDateStr = slashCommandInteraction.getOptionByName("EndDate").get().getStringValue().get();

        LOGGER.info(String.format("Running UserWeeklyRaidReportCommand (BungieID:%s | StartDate:%s | EndDate:%s)",
                bungieId, startDateStr, endDateStr));

        slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {

            interactionOriginalResponseUpdater.setContent(String
                    .format("Building a weekly raid report from %s to %s for %s", startDateStr, endDateStr, bungieId))
                    .update();

            try {
                LocalDate startDate = LocalDate.parse(startDateStr, DateTimeFormatter.BASIC_ISO_DATE);
                LocalDate endDate = LocalDate.parse(endDateStr, DateTimeFormatter.BASIC_ISO_DATE);
                String userReport = RaidReportTool.getUserWeeklyClears(bungieId, startDate, endDate);
                if (userReport.isEmpty()) {
                    interactionOriginalResponseUpdater.setContent("").addEmbed(new EmbedBuilder()
                            .setTitle(String.format("%s Raid Report from %s to %s", bungieId, startDate.toString(),
                                    endDate.toString()))
                            .setDescription(
                                    "An Error occured Finding this Guardian. Please make sure to provide the full BungieID (Guardian#0000)")
                            .setFooter("ERROR")
                            .setThumbnail(getClass().getClassLoader().getResourceAsStream("thumbnail.jpg"))
                            .setColor(Color.RED)).update();
                } else {
                    interactionOriginalResponseUpdater.addEmbed(new EmbedBuilder()
                            .setTitle(String.format("%s Raid Report from %s to %s", bungieId, startDate.toString(),
                                    endDate.toString()))
                            .setDescription(userReport).setFooter("Happy Raiding!")
                            .setThumbnail(getClass().getClassLoader().getResourceAsStream("thumbnail.jpg"))
                            .setColor(Color.GREEN)).update();
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                interactionOriginalResponseUpdater.setContent("").addEmbed(new EmbedBuilder()
                        .setTitle(String.format("%s Raid Report from %s to %s", bungieId, startDateStr, endDateStr))
                        .setDescription(
                                "An Error occured Finding this Guardian. Please make sure to provide the full BungieID (Guardian#0000)")
                        .setFooter("ERROR")
                        .setThumbnail(getClass().getClassLoader().getResourceAsStream("thumbnail.jpg"))
                        .setColor(Color.RED)).update();
            }
        });
    }

}
