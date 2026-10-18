package com.tizem.chatlimiter;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatLimiterPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private final Map<UUID, Long> lastChat = new ConcurrentHashMap<>();
    private volatile long cooldownMs;
    private volatile String prefix;
    private volatile String arrow;
    private volatile TextColor prefixColor;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        load();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("chatlimiter").setExecutor(this);
        getCommand("chatlimiter").setTabCompleter(this);
    }

    private void load() {
        reloadConfig();
        cooldownMs = (long) (getConfig().getDouble("cooldown", 2.0) * 1000);
        prefix = getConfig().getString("prefix", "CHAT");
        arrow = getConfig().getString("arrow", "➟");
        TextColor c = TextColor.fromHexString(getConfig().getString("prefix-color", "#1FD8FF"));
        prefixColor = c != null ? c : NamedTextColor.AQUA;
    }

    private Component line(Component body) {
        return Component.text(prefix, prefixColor, TextDecoration.BOLD)
                .append(Component.text(" " + arrow + " ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                .append(body.decoration(TextDecoration.BOLD, false));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission("tizem.chatlimiter.bypass")) return;

        long now = System.currentTimeMillis();
        Long last = lastChat.get(p.getUniqueId());
        if (last != null) {
            long remaining = cooldownMs - (now - last);
            if (remaining > 0) {
                event.setCancelled(true);
                String secs = String.format(Locale.US, "%.2f", remaining / 1000.0);
                p.sendMessage(line(Component.text("Please wait " + secs + "s to send another message!", NamedTextColor.WHITE)));
                return;
            }
        }
        lastChat.put(p.getUniqueId(), now);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastChat.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            load();
            sender.sendMessage(line(Component.text("Config reloaded!", NamedTextColor.WHITE)));
        } else {
            sender.sendMessage(line(Component.text("/chatlimiter reload", NamedTextColor.WHITE)));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return args.length == 1 ? List.of("reload") : List.of();
    }
}
