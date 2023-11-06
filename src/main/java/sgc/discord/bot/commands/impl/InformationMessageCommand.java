package sgc.discord.bot.commands.impl;

import java.awt.Color;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.slf4j.Logger;

import sgc.discord.bot.BotApplication;
import sgc.discord.bot.commands.Command;
import sgc.discord.infographics.GoogleDriveUtil;
import sgc.discord.messages.Message;

public class InformationMessageCommand implements Command {

        private static final Logger LOGGER = BotApplication.getLogger();

        @Override
        public void handle(SlashCommandInteraction slashCommandInteraction) {
                String value = slashCommandInteraction.getOptionByName("InfomationMessage").get().getStringValue()
                                .get();
                Message message = Message.getFromName(value);
                LOGGER.info("Running InfographicCommand with option "
                                + value);

                slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {

                        try {
                                byte[] sgcLogo = GoogleDriveUtil.getSGCLogo();

                                interactionOriginalResponseUpdater.setContent("")
                                                .addEmbed(new EmbedBuilder()
                                                                .setTitle(message.title)
                                                                .setDescription(message.body)
                                                                .setFooter("Shrouded Gaming | Twitch.tv/ShroudedGaming | #AreYouShrouded")
                                                                .setThumbnail(sgcLogo)
                                                                .setColor(Color.CYAN)
                                                                .setUrl(message.url))
                                                .update();

                        } catch (Exception e) {
                                LOGGER.error(e.getMessage(), e);
                                interactionOriginalResponseUpdater.setContent("")
                                                .addEmbed(new EmbedBuilder().setTitle("")
                                                                .setDescription("An error occured while fetching the information message for "
                                                                                + value)
                                                                .setFooter("ERROR")
                                                                .setThumbnail(getClass().getClassLoader()
                                                                                .getResourceAsStream("thumbnail.jpg"))
                                                                .setColor(Color.RED))
                                                .update();
                        }
                });
        }

}
