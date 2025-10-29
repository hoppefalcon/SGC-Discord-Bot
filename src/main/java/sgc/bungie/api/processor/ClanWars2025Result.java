package sgc.bungie.api.processor;

public class ClanWars2025Result {
    private int fireteamOpsCount = 0; // MODE 3
    private int pinnacleOpsCount = 0; // MODE 3
    private int crucibleOpsCount = 0; // MODE 5
    private int weeklyMythicCount = 0; // MODE 2
    private int raidCount = 0;

    public ClanWars2025Result() {
    }

    public int getFireteamOpsCount() {
        return fireteamOpsCount;
    }

    public int getPinnacleOpsCount() {
        return pinnacleOpsCount;
    }

    public int getCrucibleOpsCount() {
        return crucibleOpsCount;
    }

    public int getWeeklyMythicCount() {
        return weeklyMythicCount;
    }

    public int getRaidCount() {
        return raidCount;
    }

    public void incrementFireteamOpsCount() {
        fireteamOpsCount++;
    }

    public void incrementPinnacleOpsCount() {
        pinnacleOpsCount++;
    }

    public void incrementCrucibleOpsCount() {
        crucibleOpsCount++;
    }

    public void incrementWeeklyMythicCount() {
        weeklyMythicCount++;
    }

    public void incrementRaidCount() {
        raidCount++;
    }

}
