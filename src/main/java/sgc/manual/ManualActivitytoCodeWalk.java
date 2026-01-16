package sgc.manual;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.RaidReportTool;
import sgc.types.Mode;

public class ManualActivitytoCodeWalk {
        private static final Logger LOGGER = LoggerFactory.getLogger(ManualActivitytoCodeWalk.class);

        public static void main(String[] args) throws Exception {

                int year = 2023;
                // LocalDate startDate = YearMonth.of(year, 1).atDay(1);
                // LocalDate endDate = YearMonth.of(year, 12).atEndOfMonth();
                LocalDate endDate = YearMonth.of(2023, 10).atDay(8);
                LocalDate startDate = YearMonth.of(2023, 2).atDay(28);

                HashMap<String, HashMap<Mode, Boolean>> activitytoCodeWalk = RaidReportTool
                                .getActivitytoCodeWalk("hoppefalcon#7599", startDate, endDate);
                Path outputPath = Paths.get("target", "Bungie_Activity_Code_Crosswalk.csv");
                Files.write(outputPath, getActivitytoCodeWalkAsCsv(activitytoCodeWalk)
                                .getBytes(StandardCharsets.UTF_8));
        }

        private static String getActivitytoCodeWalkAsCsv(HashMap<String, HashMap<Mode, Boolean>> activitytoCodeWalk)
                        throws IOException {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("\"Activity Name\",");
                for (Mode mode : Mode.validModesForPOTW()) {
                        stringBuilder.append("\"" + mode.name + "\",");
                }
                stringBuilder.append("\n");
                activitytoCodeWalk.forEach((name, map) -> {
                        if (name != null && !name.isEmpty()) {
                                stringBuilder.append("\"" + name + "\",");

                                for (Mode mode : Mode.validModesForPOTW()) {
                                        if (map.get(mode)) {
                                                stringBuilder.append("\"X\",");
                                        } else {
                                                stringBuilder.append("\"\",");
                                        }
                                }
                                stringBuilder.append("\n");
                        }
                });

                return stringBuilder.toString();
        }
}
