/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.discord.infographics;

/**
 * @author chris hoppe
 */
public enum Infographic {
    POTW("1vne74ymV4yYl6aJJASCF624Frdn4BoQA"),
    ROLES("1EIHyoAy0bo5y6zSXX91tJZE2lJvFvrQI");

    public final String folderID;

    private Infographic(String folderID) {
        this.folderID = folderID;
    }

}
