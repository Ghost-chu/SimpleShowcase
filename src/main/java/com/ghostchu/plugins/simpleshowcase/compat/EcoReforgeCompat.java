package com.ghostchu.plugins.simpleshowcase.compat;

import com.willfp.eco.core.display.DisplayProperties;
import com.willfp.reforges.ReforgesPlugin;
import com.willfp.reforges.display.ReforgesDisplay;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EcoReforgeCompat implements CompatHandler {
    private ReforgesDisplay display;

    public EcoReforgeCompat() {
        if (Bukkit.getPluginManager().isPluginEnabled("Reforges")) {
            display = new ReforgesDisplay(ReforgesPlugin.getInstance());
        }
    }

    @Override
    public ItemStack handle(ItemStack itemStack, Player player) {
        if (display == null) {
           return itemStack;
        }
        ItemStack stack = itemStack.clone();
        display.display(stack, player, display.generateVarArgs(stack));
        display.display(stack, player, new DisplayProperties(false,false,stack), display.generateVarArgs(stack));
        return stack;
    }
}
