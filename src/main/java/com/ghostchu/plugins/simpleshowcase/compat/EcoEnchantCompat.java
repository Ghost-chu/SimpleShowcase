package com.ghostchu.plugins.simpleshowcase.compat;

import com.willfp.eco.core.display.DisplayProperties;
import com.willfp.ecoenchants.EcoEnchantsPlugin;
import com.willfp.ecoenchants.display.EnchantDisplay;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EcoEnchantCompat implements CompatHandler {
    private EnchantDisplay display;

    public EcoEnchantCompat() {
        if (Bukkit.getPluginManager().isPluginEnabled("EcoEnchants")) {
            display = new EnchantDisplay(EcoEnchantsPlugin.getInstance());
        }
    }

    @Override
    public ItemStack handle(ItemStack itemStack, Player player) {
        if (display == null) {
           return  itemStack;
        }
        ItemStack stack = itemStack.clone();
        display.display(stack, player, display.generateVarArgs(stack));
        display.display(stack, player, new DisplayProperties(false,false,stack), display.generateVarArgs(stack));
        return stack;
    }
}
