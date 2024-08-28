package fr.iban.bukkitcore.listeners;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import fr.iban.bukkitcore.CoreBukkitPlugin;
import fr.iban.common.manager.GlobalLoggerManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CommandsListener implements Listener {

    private final CoreBukkitPlugin plugin;
    private final ConcurrentHashMap<UUID, Set<String>> approvedCommands = new ConcurrentHashMap<>();

    public CommandsListener(CoreBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent e) {
        Player player = e.getPlayer();

        if (player.hasPermission("servercore.admin")) {
            return;
        }

        Set<String> allowed = ConcurrentHashMap.newKeySet();
        allowed.addAll(plugin.getTrustedCommandManager().getBukkitPlayerCommands());

        if (player.hasPermission("servercore.moderation")) {
            allowed.addAll(plugin.getTrustedCommandManager().getBukkitStaffCommands());
        }

        e.getCommands().clear();
        e.getCommands().addAll(allowed);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();
        String ip = player.getAddress() != null ? player.getAddress().getHostString() : "unknown";

        if (!plugin.getConfig().getBoolean("command-approval", true)) {
            return;
        }

        if (plugin.getTrustedUserManager().isTrusted(player)) {
            return;
        }

        String command = e.getMessage().split(" ")[0].replace("/", "");

        if (plugin.getTrustedCommandManager().getTrustedBukkitCommands().contains(command.toLowerCase())) {
            return;
        }

        approvedCommands.putIfAbsent(player.getUniqueId(), ConcurrentHashMap.newKeySet());
        if (approvedCommands.get(player.getUniqueId()).contains(command)) {
            approvedCommands.get(player.getUniqueId()).remove(command);
            return;
        }
        
        Command bukkitCommand = Bukkit.getCommandMap().getCommand(command);
        if (bukkitCommand != null) {
            if (!bukkitCommand.testPermission(player)) return;
            e.setCancelled(true);
            player.sendMessage("§cApprobation requise.");
            plugin.getApprovalManager().sendRequest(player,
                    player.getName() + " (" + ip + ") essaye d'exécuter la commande " + e.getMessage() + ".",
                    result -> {
                        if (result) {
                            plugin.runRegionTask(player.getLocation(), () -> {
                                approvedCommands.get(player.getUniqueId()).add(command);
                                player.chat(e.getMessage());
                            });
                        }
                    }
            );
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandLogger(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();

        if (e.isCancelled()) return;

        GlobalLoggerManager.saveLog(plugin.getServerName(), player.getName() + " issued server command: " + e.getMessage() + ".");
    }
}
