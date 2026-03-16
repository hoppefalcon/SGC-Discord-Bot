package sgc.discord.bot.commands.impl;

import java.awt.Color;
import java.util.HashMap;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.slf4j.Logger;

import com.google.api.services.sheets.v4.model.ValueRange;

import sgc.discord.bot.BotApplication;
import sgc.discord.bot.commands.Command;
import sgc.discord.infographics.GoogleDriveUtil;

public class EventLeaderboardCommand implements Command {

    private static final Logger LOGGER = BotApplication.getLogger();

    @Override
    public void handle(SlashCommandInteraction slashCommandInteraction) {
        System.gc();

        slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {
            interactionOriginalResponseUpdater.setContent("Building a redeemable list").update();

            try {
                String response = getZ2HLeaderboard();

                interactionOriginalResponseUpdater.addEmbed(new EmbedBuilder().setTitle("Event Leaderboard")
                        .setDescription(response).setFooter("#AreYouShrouded")
                        .setThumbnail(getClass().getClassLoader().getResourceAsStream("thumbnail.jpg"))
                        .setColor(Color.GREEN)).update();

            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                interactionOriginalResponseUpdater.setContent("")
                        .addEmbed(new EmbedBuilder().setTitle("Event Leaderboard").setDescription(
                                "An Error occured.")
                                .setFooter("ERROR")
                                .setThumbnail(getClass().getClassLoader().getResourceAsStream("thumbnail.jpg"))
                                .setColor(Color.RED))
                        .update();
            }
        });
    }

    private String getZ2HLeaderboard() {
        HashMap<String, Double> teamClearPercentages = new HashMap<>();
        StringBuilder response = new StringBuilder();
        ValueRange rawData = GoogleDriveUtil.getSheetData("1O7e-gOh8v5QUOCCSUO9sDL5gGkK6luNSE2sbN8ZqpUY",
                "Compiled!A2:V");
        rawData.getValues().stream().forEach(row -> {
            String teamName = row.get(0).toString();
            int clears = 0;
            for (int i = 1; i < row.size(); i++) {
                if (Integer.parseInt(row.get(i).toString()) > 0) {
                    clears++;
                }
            }
            teamClearPercentages.put(teamName, clears / (double) (row.size() - 1));
        });
        teamClearPercentages.entrySet()
                .stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .forEach(entry -> response
                        .append(String.format("%s: %.2f%%\n", entry.getKey(), entry.getValue() * 100)));

        return response.toString();
    }
}
