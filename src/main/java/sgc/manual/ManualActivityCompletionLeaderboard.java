package sgc.manual;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.Clan;
import sgc.bungie.api.processor.Member;
import sgc.bungie.api.processor.RaidReportTool;
import sgc.types.Mode;

public class ManualActivityCompletionLeaderboard {
        private static final Logger LOGGER = LoggerFactory.getLogger(ManualActivityCompletionLeaderboard.class);

        public static void main(String[] args) throws Exception {

                List<Clan> clanList = RaidReportTool.initializeClanList();
                HashMap<String, Member> sgcClanMembersMap = RaidReportTool.initializeClanMembersMap(clanList);
                LocalDate endDate = YearMonth.of(2026, 4).atDay(21);
                LocalDate startDate = YearMonth.of(2026, 4).atDay(14);
                List<Integer> modes = new ArrayList<Integer>();
                modes.add(Mode.IRON_BANNER_CLASH.getValue());
                modes.add(Mode.IRON_BANNER_CONTROL.getValue());

                HashMap<Member, Integer> clearedActivitiesByMode = RaidReportTool
                                .getClearedActivitiesByMode(sgcClanMembersMap, startDate, endDate, modes);

                StringBuilder response = new StringBuilder();
                clearedActivitiesByMode.entrySet().stream()
                                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                                .forEach(m -> response
                                                .append(String.format("\"%s\",%s,%d\n",
                                                                m.getKey().getCombinedBungieGlobalDisplayName(),
                                                                m.getKey().getClan().getCallsign(),
                                                                m.getValue())));
                Path outputPath = Paths.get("target", "ActivityCompletionLeaderboard.csv");
                Files.write(outputPath, response.toString()
                                .getBytes(StandardCharsets.UTF_8));
                System.out.println(response.toString());
        }

}