package sgc.discord.bot.commands.impl;

import java.awt.Color;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.slf4j.Logger;

import sgc.discord.bot.BotApplication;
import sgc.discord.bot.commands.Command;
import sgc.bungie.api.processor.RaidReportTool;

public class UserRaidReportCommand implements Command {

    private static final Logger LOGGER = BotApplication.getLogger();

    @Override
    public void handle(SlashCommandInteraction slashCommandInteraction) {
        String bungieID = slashCommandInteraction.getOptionByName("BungieID").get().getStringValue().get();
        LOGGER.info("Running UserRaidReportCommand with BungieID " + bungieID);

        slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {

            interactionOriginalResponseUpdater.setContent("Building a raid report for " + bungieID).update();

            try {
                String userReport = RaidReportTool.getUserReport(bungieID);
                if (userReport.isEmpty()) {
                    interactionOriginalResponseUpdater.setContent("")
                            .addEmbed(new EmbedBuilder().setTitle(bungieID + " Raid Report").setDescription(
                                    "An Error occured Finding this Guardian. Please make sure to provide the full BungieID (Guardian#0000)")
                                    .setFooter("ERROR")
                                    .setThumbnail(getClass().getClassLoader().getResourceAsStream("thumbnail.jpg"))
                                    .setColor(Color.RED))
                            .update();
                } else {
                    interactionOriginalResponseUpdater.addEmbed(new EmbedBuilder().setTitle(bungieID + " Raid Report")
                            .setDescription(userReport).setFooter("Happy Raiding!")
                            .setThumbnail(getClass().getClassLoader().getResourceAsStream("thumbnail.jpg"))
                            .setColor(Color.GREEN)).update();
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                interactionOriginalResponseUpdater.setContent("")
                        .addEmbed(new EmbedBuilder().setTitle(bungieID + " Raid Report").setDescription(
                                "An Error occured Finding this Guardian. Please make sure to provide the full BungieID (Guardian#0000)")
                                .setFooter("ERROR")
                                .setThumbnail(getClass().getClassLoader().getResourceAsStream("thumbnail.jpg"))
                                .setColor(Color.RED))
                        .update();
            }
        });
    }

}
