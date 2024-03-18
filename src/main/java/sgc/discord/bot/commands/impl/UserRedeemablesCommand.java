package sgc.discord.bot.commands.impl;

import java.awt.Color;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.slf4j.Logger;

import sgc.discord.bot.BotApplication;
import sgc.discord.bot.commands.Command;
import sgc.bungie.api.processor.RaidReportTool;

public class UserRedeemablesCommand implements Command {

    private static final Logger LOGGER = BotApplication.getLogger();

    @Override
    public void handle(SlashCommandInteraction slashCommandInteraction) {
        System.gc();
        String bungieID = slashCommandInteraction.getOptionByName("BungieID").get().getStringValue().get();
        LOGGER.info("Running UserRedeemablesCommand with BungieID " + bungieID);

        slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {
            interactionOriginalResponseUpdater.setContent("Building a redeemable list for " + bungieID).update();

            try {
                String missingList = RaidReportTool.getMemberMissingRedeemableCollectables(bungieID);
                String nonCollectables = RaidReportTool.getNonCollectableRedeemables();
                StringBuilder response = new StringBuilder();
                if (!missingList.isEmpty()) {
                    response.append("These Collectables have not been redeemed").append(missingList);
                }
                response.append("These Collectables may or may not have been redeemed").append(nonCollectables);

                interactionOriginalResponseUpdater.addEmbed(new EmbedBuilder().setTitle(bungieID + " Raid Report")
                        .setDescription(response.toString()).setFooter("#AreYouShrouded")
                        .setThumbnail(getClass().getClassLoader().getResourceAsStream("thumbnail.jpg"))
                        .setColor(Color.GREEN)).update();

            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                interactionOriginalResponseUpdater.setContent("")
                        .addEmbed(new EmbedBuilder().setTitle(bungieID + " Redeemable List").setDescription(
                                "An Error occured Finding this Guardian. Please make sure to provide the full BungieID (Guardian#0000)")
                                .setFooter("ERROR")
                                .setThumbnail(getClass().getClassLoader().getResourceAsStream("thumbnail.jpg"))
                                .setColor(Color.RED))
                        .update();
            }
        });
    }

}
