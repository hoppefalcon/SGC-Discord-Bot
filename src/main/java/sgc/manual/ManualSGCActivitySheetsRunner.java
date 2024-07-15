package sgc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.activity.ActivityReportTool;
import sgc.discord.infographics.GoogleDriveUtil;

public class ManualSGCActivitySheetsRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManualSGCActivitySheetsRunner.class);
    private static ExecutorService executorService = Executors.newFixedThreadPool(15);
    private static final DiscordApi API = new DiscordApiBuilder().setToken(System.getenv("DISCORD_TOKEN"))
            .setAllIntents().login().join();

    public static void main(String[] args) throws InterruptedException, IOException {
        // System.out.println(System.getenv("GOOGLE_API_PRIVATE_KEY"));
        // String encodedString =
        // Base64.getEncoder().encodeToString(System.getenv("GOOGLE_API_PRIVATE_KEY").getBytes());
        // System.out.println(encodedString);
        // System.out.println(new String(Base64.getDecoder().decode(encodedString),
        // StandardCharsets.UTF_8));

        GoogleDriveUtil.initiateGoogleSheetsAuth();
        ActivityReportTool.setDiscordAPI(API);
        ActivityReportTool.runActivitySheets();
    }
}
