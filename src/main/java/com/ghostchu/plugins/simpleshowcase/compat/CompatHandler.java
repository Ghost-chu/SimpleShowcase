package com.ghostchu.plugins.simpleshowcase.compat;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface CompatHandler {
    ItemStack handle(ItemStack itemStack, Player player);
}
