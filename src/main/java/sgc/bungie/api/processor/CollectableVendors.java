/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.bungie.api.processor;

import java.util.Arrays;
import java.util.List;

/**
 * @author chris hoppe
 */
public enum CollectableVendors {
    XUR("Xûr", Arrays.asList("2190858386", "3751514131", "537912098")),
    ADA_1("Ada-1", Arrays.asList("350061650"));

    public final String name;
    private final List<String> vendorHashes;

    private CollectableVendors(String name, List<String> vendorHashes) {
        this.name = name;
        this.vendorHashes = vendorHashes;
    }

    public String getName() {
        return name;
    }
}
