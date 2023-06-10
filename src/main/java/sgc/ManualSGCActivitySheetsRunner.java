package sgc;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.activity.ActivityReportTool;

public class ManualSGCActivitySheetsRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManualSGCActivitySheetsRunner.class);
    private static ExecutorService executorService = Executors.newFixedThreadPool(15);
    private static final DiscordApi API = new DiscordApiBuilder().setToken(System.getenv("DISCORD_TOKEN"))
            .setAllIntents().login().join();

    public static void main(String[] args) throws InterruptedException, IOException {
        // System.out.println(System.getenv("DISCORD_TOKEN"));
        ActivityReportTool.setDiscordAPI(API);
        ActivityReportTool.runActivitySheets();
    }
}
