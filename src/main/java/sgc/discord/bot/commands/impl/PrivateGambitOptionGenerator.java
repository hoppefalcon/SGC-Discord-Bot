package sgc.discord.bot.commands.impl;

import java.awt.Color;

import org.apache.commons.lang3.tuple.Pair;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.slf4j.Logger;

import sgc.discord.bot.BotApplication;
import sgc.discord.bot.commands.Command;
import sgc.bungie.api.processor.RaidReportTool;

public class PrivateGambitOptionGenerator implements Command {

        private static final Logger LOGGER = BotApplication.getLogger();

        @Override
        public void handle(SlashCommandInteraction slashCommandInteraction) {
                System.gc();
                LOGGER.info("Running Private Gambit Option Generator");

                slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {
                        interactionOriginalResponseUpdater
                                        .setContent("Running Private Gambit Option Generator").update();

                        try {
                                Pair<String, String> randomPrivateGambitOptions = RaidReportTool
                                                .getRandomPrivateGambitOptions();
                                if (randomPrivateGambitOptions == null) {
                                        interactionOriginalResponseUpdater.setContent("")
                                                        .addEmbed(new EmbedBuilder()
                                                                        .setTitle("Private Gambit Option Generator")
                                                                        .setDescription(
                                                                                        "An Error occured Generating Private Gambit Options")
                                                                        .setFooter("ERROR")
                                                                        .setThumbnail(getClass().getClassLoader()
                                                                                        .getResourceAsStream(
                                                                                                        "thumbnail.jpg"))
                                                                        .setColor(Color.RED))
                                                        .update();
                                } else {
                                        interactionOriginalResponseUpdater
                                                        .addEmbed(new EmbedBuilder()
                                                                        .setTitle("Private Gambit Option Generator")
                                                                        .setDescription(String.format(
                                                                                        "Map: %s\nCombatant: %s",
                                                                                        randomPrivateGambitOptions
                                                                                                        .getLeft(),
                                                                                        randomPrivateGambitOptions
                                                                                                        .getRight()))
                                                                        .setFooter("Lock 'n Load, Guardian!")
                                                                        .setThumbnail(getClass().getClassLoader()
                                                                                        .getResourceAsStream(
                                                                                                        "thumbnail.jpg"))
                                                                        .setColor(Color.GREEN))
                                                        .update();
                                }
                        } catch (Exception e) {
                                LOGGER.error(e.getMessage(), e);
                                interactionOriginalResponseUpdater.setContent("")
                                                .addEmbed(new EmbedBuilder()
                                                                .setTitle("Private Gambit Option Generator")
                                                                .setDescription("An Error occured Generating Private Gambit Options")
                                                                .setFooter("ERROR")
                                                                .setThumbnail(getClass().getClassLoader()
                                                                                .getResourceAsStream("thumbnail.jpg"))
                                                                .setColor(Color.RED))
                                                .update();
                        }
                });
        }

}
