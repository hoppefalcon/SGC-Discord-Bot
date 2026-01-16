/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.bungie.api.processor;

import sgc.types.Dungeon;

/**
 * @author chris hoppe
 */
public class DungeonActivity {

    private final Dungeon dungeon;
    private int totalClears = 0;
    private int weeklyClears = 0;

    public DungeonActivity(Dungeon dungeon) {
        this.dungeon = dungeon;
    }

    public void addClears(int newClears) {
        totalClears += newClears;
    }

    public void addClears(double newClears) {
        totalClears += newClears;
    }

    public Dungeon getDungeon() {
        return dungeon;
    }

    public int getTotalClears() {
        return totalClears;
    }

    public void addWeeklyClears(int newClears) {
        weeklyClears += newClears;
    }

    public void addWeeklyClears(double newClears) {
        weeklyClears += newClears;
    }

    /**
     * @return the weeklyClears
     */
    public int getWeeklyClears() {
        return weeklyClears;
    }

}
