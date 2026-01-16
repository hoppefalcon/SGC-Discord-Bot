package sgc.discord.bot.commands.impl;

import java.awt.Color;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.slf4j.Logger;

import sgc.discord.bot.BotApplication;
import sgc.discord.bot.commands.Command;
import sgc.types.RedeemableCollectable;
import sgc.bungie.api.processor.RaidReportTool;

public class AllRedeemablesCommand implements Command {

    private static final Logger LOGGER = BotApplication.getLogger();

    @Override
    public void handle(SlashCommandInteraction slashCommandInteraction) {
        System.gc();

        slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {
            interactionOriginalResponseUpdater.setContent("Building a redeemable list").update();

            try {
                String collectableList = RaidReportTool
                        .getRedeemableCollectableList(RedeemableCollectable.getAllCollectableHashes());
                String nonCollectables = RaidReportTool.getNonCollectableRedeemables();
                StringBuilder response = new StringBuilder();
                response.append(collectableList).append(nonCollectables);

                interactionOriginalResponseUpdater.addEmbed(new EmbedBuilder().setTitle("Redeemables List")
                        .setDescription(response.toString()).setFooter("#AreYouShrouded")
                        .setThumbnail(getClass().getClassLoader().getResourceAsStream("thumbnail.jpg"))
                        .setColor(Color.GREEN)).update();

            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                interactionOriginalResponseUpdater.setContent("")
                        .addEmbed(new EmbedBuilder().setTitle("Redeemable List").setDescription(
                                "An Error occured.")
                                .setFooter("ERROR")
                                .setThumbnail(getClass().getClassLoader().getResourceAsStream("thumbnail.jpg"))
                                .setColor(Color.RED))
                        .update();
            }
        });
    }

}
