package sgc.discord.bot.commands.impl;

import java.awt.Color;

import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.slf4j.Logger;

import sgc.discord.bot.BotApplication;
import sgc.discord.bot.commands.Command;
import sgc.discord.infographics.GoogleDriveUtil;

public class InfographicCommand implements Command {

        private static final Logger LOGGER = BotApplication.getLogger();

        @Override
        public void handle(SlashCommandInteraction slashCommandInteraction) {
                String infographic = slashCommandInteraction.getOptionByName("Infographic").get().getStringValue()
                                .get();
                String infoName = slashCommandInteraction.getOptionByName("Infographic").get().getName();
                LOGGER.info("Running InfographicCommand with option "
                                + infoName);

                slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {

                        try {
                                byte[] infographicFile = GoogleDriveUtil.getInfographic(infographic);

                                interactionOriginalResponseUpdater.setContent("")
                                                .addEmbed(new EmbedBuilder()
                                                                .setFooter("Shrouded Gaming | Twitch.tv/ShroudedGaming | #AreYouShrouded")
                                                                .setImage(infographicFile)
                                                                .setColor(Color.ORANGE))
                                                .update();

                        } catch (Exception e) {
                                LOGGER.error(e.getMessage(), e);
                                interactionOriginalResponseUpdater.setContent("")
                                                .addEmbed(new EmbedBuilder().setTitle(infoName + " Infographic")
                                                                .setDescription("An error occured while fetching the infographic for "
                                                                                + infoName)
                                                                .setFooter("ERROR")
                                                                .setThumbnail(getClass().getClassLoader()
                                                                                .getResourceAsStream("thumbnail.jpg"))
                                                                .setColor(Color.RED))
                                                .update();
                        }
                });
        }

}
