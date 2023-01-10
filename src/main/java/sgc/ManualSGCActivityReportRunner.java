package sgc;

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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.Clan;
import sgc.bungie.api.processor.Member;
import sgc.bungie.api.processor.RaidReportTool;

public class Manual {
    private static final Logger LOGGER = LoggerFactory.getLogger(Manual.class);
    private static ExecutorService executorService = Executors.newFixedThreadPool(15);

    public static void main(String[] args) throws InterruptedException, IOException {
        List<Clan> clanList = RaidReportTool.initializeClanList();
        HashMap<String, Member> sgcClanMembersMap = RaidReportTool.initializeClanMembersMap(clanList);

        int year = 2022;
        LocalDate firstOfYear = YearMonth.of(year, 1).atDay(1);
        LocalDate endOfYear = YearMonth.of(year, 12).atEndOfMonth();

        LOGGER.info(String.format("Starting %s to %s SGC Activity Report", firstOfYear.toString(),
                endOfYear.toString()));

        for (int i = 0; i < clanList.size(); i++) {
            Clan clan = clanList.get(i);

            List<Callable<Object>> tasks = new ArrayList<>();
            LOGGER.info("Starting to process " + clan.getCallsign());
            clan.getMembers().forEach((memberId, member) -> {
                tasks.add(() -> {
                    if (member.hasNewBungieName()) {
                        try {
                            LOGGER.debug("Starting to process " + member.getDisplayName());
                            RaidReportTool.getMembersClearedActivities(member,
                                    firstOfYear,
                                    endOfYear,
                                    sgcClanMembersMap);
                            LOGGER.debug("Finished processing " + member.getDisplayName());
                        } catch (IOException ex) {
                            LOGGER.error("Error processing " + member.getDisplayName(), ex);
                        }
                    }
                    System.gc();
                    return null;
                });
            });

            try {
                executorService.invokeAll(tasks);
            } finally {
                System.gc();
                LOGGER.info("Finished processing " + clan.getCallsign());
            }

        }
        LOGGER.info(String.format("Finished %s to %s SGC Activity Report", firstOfYear.toString(),
                endOfYear.toString()));

        HashMap<Platform, String> potwActivityReportAsCsv = RaidReportTool.getPlatformActivityReportsAsCsv(clanList);
        Path xboxPath = Paths.get("target", Platform.XBOX.getName() +
                "_annual_CPOTW.csv");
        Files.write(xboxPath,
                potwActivityReportAsCsv.get(Platform.XBOX).getBytes(StandardCharsets.UTF_8));
        Path pcPath = Paths.get("target", Platform.PC.getName() +
                "_annual_CPOTW.csv");
        Files.write(pcPath,
                potwActivityReportAsCsv.get(Platform.PC).getBytes(StandardCharsets.UTF_8));
        Path psnPath = Paths.get("target", Platform.PSN.getName() +
                "_annual_CPOTW.csv");
        Files.write(psnPath,
                potwActivityReportAsCsv.get(Platform.PSN).getBytes(StandardCharsets.UTF_8));

        LOGGER.info("SGC Annual Activity Report Complete");
    }
}
