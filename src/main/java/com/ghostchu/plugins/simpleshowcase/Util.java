package com.ghostchu.plugins.simpleshowcase;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Util {
    public static String itemStackSerialize(ItemStack itemStack) {
        // yaml serialization
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("item", itemStack);
        return configuration.saveToString();
    }

    public static ItemStack itemStackDeserialize(String yaml){
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(yaml);
            return configuration.getItemStack("item");
        } catch (InvalidConfigurationException e) {
            Logger.getLogger("SimpleShowcase").log(Level.WARNING, "Failed to parse ItemStack: "+yaml, e);
            return new ItemStack(Material.AIR);
        }
    }
}
