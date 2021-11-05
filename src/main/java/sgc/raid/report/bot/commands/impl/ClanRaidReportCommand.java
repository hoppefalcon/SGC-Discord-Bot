package sgc.raid.report.bot.commands.impl;

import java.awt.Color;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.slf4j.Logger;

import sgc.raid.report.bot.BotApplication;
import sgc.raid.report.bot.commands.Command;
import sgc.sherpa.sheets.Clan;
import sgc.sherpa.sheets.RaidReportTool;

public class ClanRaidReportCommand implements Command {

    private static final Logger LOGGER = BotApplication.getLogger();

    @Override
    public void handle(SlashCommandInteraction slashCommandInteraction) {
        String bungieClanID = slashCommandInteraction.getOptionByName("Clan").get().getStringValue().get();

        slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {

            interactionOriginalResponseUpdater.setContent("Building a clan raid report for " + bungieClanID).update();

            try {
                Clan clan = RaidReportTool.getClanInformation(bungieClanID);
                interactionOriginalResponseUpdater.setContent("Building a clan raid report for " + clan.getName())
                        .update();

                RaidReportTool.getClanRaidReport(clan, interactionOriginalResponseUpdater);

                new MessageBuilder()
                        .addEmbed(new EmbedBuilder().setAuthor(slashCommandInteraction.getUser())
                                .setTitle(clan.getName() + " Raid Report").setDescription("Raid Report Completed")
                                .setFooter("Happy Raiding!")
                                .setThumbnail(getClass().getClassLoader().getResourceAsStream("thumbnail.jpg"))
                                .setColor(Color.CYAN))
                        .addAttachment(RaidReportTool.getClanRaidReportAsCsvByteArray(clan),
                                String.format("%s [%s].csv", clan.getName(),
                                        LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)))
                        .send(slashCommandInteraction.getChannel().get());

            } catch (Exception e) {
                // TODO Auto-generated catch block
                LOGGER.error(e.getMessage(), e);
                interactionOriginalResponseUpdater
                        .setContent("An error occured while building a Raid Report for " + bungieClanID).update();
            }
        });
    }

}
