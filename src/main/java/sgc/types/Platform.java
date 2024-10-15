/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.types;

/**
 * @author chris hoppe
 */
public enum Platform {
    PC("PC"),
    XBOX("XBOX"),
    PSN("PSN");

    private final String name;

    private Platform(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

}
