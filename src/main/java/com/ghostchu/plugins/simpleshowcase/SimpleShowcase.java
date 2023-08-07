package com.ghostchu.plugins.simpleshowcase;

import com.ghostchu.plugins.simpleshowcase.compat.CompatHandler;
import com.ghostchu.plugins.simpleshowcase.compat.EcoEnchantCompat;
import com.ghostchu.plugins.simpleshowcase.compat.EcoReforgeCompat;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SimpleShowcase extends JavaPlugin implements Listener {
    private List<CompatHandler> handlers = new ArrayList<>();
    private JedisPool pool;
    private final String REDIS_PREFIX = "SimpleShowcase-PreviewEntry-";

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        connectToRedis();
        handlers.add(new EcoEnchantCompat());
        handlers.add(new EcoReforgeCompat());
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
    }

    private void connectToRedis() {
        pool = new JedisPool(getConfig().getString("redis.host", "localhost"), getConfig().getInt("redis.host", 6379));
    }

    public Jedis getJedis() {
        Jedis jedis = pool.getResource();
        jedis.auth(getConfig().getString("redis.pass", ""));
        jedis.select(getConfig().getInt("redis.database", 0));
        return jedis;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        pool.close();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("只有玩家可以这样做");
            return true;
        }
        if (args.length == 1) {
            try {
                handleInventoryPreview(player, args[0]);
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("无效的预览ID").color(NamedTextColor.RED));
            }
            return true;
        } else if (args.length == 0) {
            ItemStack stack = player.getInventory().getItemInMainHand().clone();
            if (stack.getType().isAir()) {
                player.sendMessage(Component.text("要展示您的物品，您需要将其拿在手中。").color(NamedTextColor.RED));
                return true;
            }
            for (CompatHandler handler : handlers) {
                stack = handler.handle(stack, player);
            }
            ItemStack finalStack = stack;
            Bukkit.getScheduler().runTaskAsynchronously(this,()->{
                UUID uuid = generatePreviewInventory(finalStack);
                Component display = handleItemDisplay(finalStack);
                Component chatMessage =
                        player.displayName().append(Component.text(" 正在展示物品：").color(NamedTextColor.GRAY))
                                .append(display.hoverEvent(finalStack.asHoverEvent())
                                        .clickEvent(ClickEvent.runCommand("/simpleshowcase:showitem " + uuid)));
                broadcastItemShowcase(chatMessage, player);
            });
            return true;
        } else {
            return false;
        }
    }

    private void broadcastItemShowcase(Component chatMessage, Player player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("MessageRaw");
        out.writeUTF("ALL");
        out.writeUTF(GsonComponentSerializer.gson().serialize(chatMessage));
        player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }

    private void handleInventoryPreview(Player player, String input) {
        Bukkit.getScheduler().runTaskAsynchronously(this, ()->{
            UUID uuid = UUID.fromString(input);
            String stackString;
            try (Jedis jedis = getJedis()) {
                stackString = jedis.get(REDIS_PREFIX + uuid);
            }
            if(StringUtils.isEmpty(stackString)){
                player.sendMessage(Component.text("物品预览已过期").color(NamedTextColor.RED));
                return;
            }
            ItemStack stack = Util.itemStackDeserialize(stackString);
            if(stack == null || stack.getType() == Material.AIR){
                player.sendMessage(Component.text("数据解析失败").color(NamedTextColor.RED));
                return;
            }

            Bukkit.getScheduler().runTask(this,()-> {
                Inventory inventory = Bukkit.createInventory(new SimpleShowcasePreviewGUIHolder(), 9, Component.text("展示物品预览"));
                for (int i = 0; i < 9; i++) {
                    inventory.setItem(i, stack);
                }
                player.openInventory(inventory);
            });
        });
    }

    private UUID generatePreviewInventory(ItemStack stack) {
        UUID uuid = UUID.randomUUID();
        try (Jedis jedis = getJedis()) {
           String response = jedis.set(REDIS_PREFIX + uuid, Util.itemStackSerialize(stack), new SetParams().nx().ex(60 * 15));
            getLogger().info("Stored stack with response: " + response);
        }
        return uuid;
    }

    private Component handleItemDisplay(ItemStack stack) {
        Component component = stack.displayName();
        if (component.equals(Component.empty())) component = Component.translatable(stack.translationKey());
        component = component.colorIfAbsent(NamedTextColor.AQUA);
        return component;
        //return Component.text("[").color(NamedTextColor.AQUA).append(component).append(Component.text("]"));
    }

    @EventHandler(ignoreCancelled = true)
    public void invEvent(InventoryInteractEvent e) {
        if (e.getInventory().getHolder() instanceof SimpleShowcasePreviewGUIHolder) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void invEvent(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof SimpleShowcasePreviewGUIHolder) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void invEvent(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof SimpleShowcasePreviewGUIHolder) {
            e.setCancelled(true);
        }
    }
}
