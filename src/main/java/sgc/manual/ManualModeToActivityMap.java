package sgc.manual;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.RaidReportTool;
import sgc.types.Mode;

public class ManualModeToActivityMap {
        private static final Logger LOGGER = LoggerFactory.getLogger(ManualSGCActivityReportRunner.class);

        public static void main(String[] args) throws InterruptedException, IOException, URISyntaxException {
                try {
                        HashMap<Mode, HashMap<String, ArrayList>> latestModeToActivityMap = RaidReportTool
                                        .getLatestModeToActivityMap();

                        final StringBuilder stringBuilder = new StringBuilder();
                        latestModeToActivityMap.keySet().forEach(s -> {
                                stringBuilder.append("\"").append(s).append("\",");
                                latestModeToActivityMap.get(s).forEach((t, u) -> {
                                        stringBuilder.append("\"").append(t).append("\",");
                                });
                                stringBuilder.append("\n");
                        });

                        Path outputPath = Paths.get("target", "Mode_to_Activity_Map.csv");
                        Files.write(outputPath, stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }
        }
}
