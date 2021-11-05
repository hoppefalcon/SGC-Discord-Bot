package sgc.raid.report.bot.commands.impl;

import java.awt.Color;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

import sgc.raid.report.bot.commands.Command;
import sgc.sherpa.sheets.RaidReportTool;

public class UserRaidReportCommand implements Command {

    @Override
    public void handle(SlashCommandInteraction slashCommandInteraction) {
        String bungieID = slashCommandInteraction.getOptionByName("BungieID").get().getStringValue().get();

        slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {

            interactionOriginalResponseUpdater.setContent("Building a raid report for " + bungieID).update();

            try {
                String userReport = RaidReportTool.getUserReport(bungieID);
                if (userReport.isEmpty()) {
                    interactionOriginalResponseUpdater.addEmbed(new EmbedBuilder().setTitle(bungieID + " Raid Report")
                            .setDescription("An Error occured Finding this Guardian").setFooter("ERROR")
                            .setThumbnail(getClass().getClassLoader().getResourceAsStream("thumbnail.jpg"))
                            .setColor(Color.GREEN)).update();
                } else {
                    interactionOriginalResponseUpdater.addEmbed(new EmbedBuilder().setTitle(bungieID + " Raid Report")
                            .setDescription(userReport).setFooter("Happy Raiding!")
                            .setThumbnail(getClass().getClassLoader().getResourceAsStream("thumbnail.jpg"))
                            .setColor(Color.GREEN)).update();
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
    }

}
