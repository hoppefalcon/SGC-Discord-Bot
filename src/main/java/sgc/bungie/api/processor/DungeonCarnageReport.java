package sgc.bungie.api.processor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import sgc.types.Dungeon;

public class DungeonCarnageReport {
    private final List<CarnageReportPlayer> players = new ArrayList<>();
    private final Dungeon dungeon;
    private final LocalDate dateCompleted;

    /**
     * @param raid
     */
    public DungeonCarnageReport(Dungeon dungeon, LocalDate dateCompleted) {
        this.dungeon = dungeon;
        this.dateCompleted = dateCompleted;
    }

    /**
     * @return the players
     */
    public List<CarnageReportPlayer> getPlayers() {
        return players;
    }

    /**
     * @return the dungeon
     */
    public Dungeon getDungeon() {
        return dungeon;
    }

    /**
     * @return the dateCompleted
     */
    public LocalDate getDateCompleted() {
        return dateCompleted;
    }

    public String getCSV() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BUNGIE ID").append(",");
        stringBuilder.append("PLATFORM").append(",");
        stringBuilder.append("CLASS").append(",");
        stringBuilder.append("COMPLETED").append(",");
        stringBuilder.append("KILLS").append(",");
        stringBuilder.append("ASSISTS").append(",");
        stringBuilder.append("OPPONENTS DEFESTED").append(",");
        stringBuilder.append("DEATHS").append(",");
        stringBuilder.append("K/D").append(",");
        stringBuilder.append("KA/D").append(",");
        stringBuilder.append("TIME").append(",");
        stringBuilder.append("\n");
        players.forEach(player -> {
            stringBuilder.append(player.getCsvOutput());
        });
        return stringBuilder.toString();
    }
}
