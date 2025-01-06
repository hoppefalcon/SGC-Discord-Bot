package sgc.discord.bot.commands.impl;

import java.awt.Color;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.slf4j.Logger;

import sgc.discord.bot.BotApplication;
import sgc.discord.bot.commands.Command;
import sgc.bungie.api.processor.RaidReportTool;

public class BalancePrivateCrucibleCommand implements Command {

    private static final Logger LOGGER = BotApplication.getLogger();

    @Override
    public void handle(SlashCommandInteraction slashCommandInteraction) {
        System.gc();
        String bungieID = slashCommandInteraction.getOptionByName("BungieID").get().getStringValue().get();
        LOGGER.info("Running Balancing the Fireteam of BungieID " + bungieID + " for Private Crucible");

        slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {
            interactionOriginalResponseUpdater
                    .setContent("Balancing the Fireteam of BungieID " + bungieID + " for Private Crucible").update();

            try {
                String fireteamBalance = RaidReportTool.getFireteamBalance(bungieID);
                if (fireteamBalance.isEmpty()) {
                    interactionOriginalResponseUpdater.setContent("")
                            .addEmbed(new EmbedBuilder().setTitle("Private Crucible Balanced Teams with SGC MMR")
                                    .setDescription(
                                            "An Error occured Finding this Guardian. Please make sure to provide the full BungieID (Guardian#0000)")
                                    .setFooter("ERROR")
                                    .setThumbnail(getClass().getClassLoader().getResourceAsStream("thumbnail.jpg"))
                                    .setColor(Color.RED))
                            .update();
                } else {
                    interactionOriginalResponseUpdater
                            .addEmbed(new EmbedBuilder().setTitle("Private Crucible Balanced Teams with SGC MMR")
                                    .setDescription(fireteamBalance).setFooter("Throw More Grenades!")
                                    .setThumbnail(getClass().getClassLoader().getResourceAsStream("thumbnail.jpg"))
                                    .setColor(Color.GREEN))
                            .update();
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                interactionOriginalResponseUpdater.setContent("")
                        .addEmbed(new EmbedBuilder().setTitle("Private Crucible Balanced Teams with SGC MMR")
                                .setDescription(
                                        "An Error occured Finding this Guardian. Please make sure to provide the full BungieID (Guardian#0000)")
                                .setFooter("ERROR")
                                .setThumbnail(getClass().getClassLoader().getResourceAsStream("thumbnail.jpg"))
                                .setColor(Color.RED))
                        .update();
            }
        });
    }

}
