package sgc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.activity.ActivityReportTool;

public class ManualUserDiscordActivityRunner {
        private static final Logger LOGGER = LoggerFactory.getLogger(ManualUserDiscordActivityRunner.class);

        private static final DiscordApi API = new DiscordApiBuilder().setToken(System.getenv("DISCORD_TOKEN"))
                        .setAllIntents().login().join();

        public static void main(String[] args) throws Exception {
                HashMap<SGC_Clan, List<String>> clans = new HashMap<>();
                clans.put(SGC_Clan.SOL, Arrays.asList(new String[] { "1039767134961684501" }));
                clans.put(SGC_Clan.KOTR, Arrays.asList(new String[] { "1027211505294385152" }));
                ActivityReportTool.setDiscordAPI(API);

                clans.keySet().forEach(clan -> {
                        String report = ActivityReportTool.getClansDiscordActivityForChannels(clan,
                                        clans.get(clan), 14);
                        Path outputPath = Paths.get("target", clan.name() + "_Discord_Activity_Report.csv");
                        try {
                                Files.write(outputPath, report.getBytes(StandardCharsets.UTF_8));
                        } catch (IOException e) {
                                LOGGER.error(e.getMessage(), e);
                        }
                });
        }
}
