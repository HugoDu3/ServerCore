package fr.iban.bukkitcore.manager;

import fr.iban.bukkitcore.CoreBukkitPlugin;
import fr.iban.common.TrustedUser;
import fr.iban.common.manager.TrustedUserManager;
import fr.iban.common.messaging.message.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BukkitTrustedUserManager extends TrustedUserManager {

    private final CoreBukkitPlugin plugin;
    private final List<TrustedUser> trustedUsers = new CopyOnWriteArrayList<>();

    public BukkitTrustedUserManager(CoreBukkitPlugin plugin) {
        this.plugin = plugin;
        loadTrustedUsers();
    }

    @Override
    public void loadTrustedUsers() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (trustedUsers) {
                super.loadTrustedUsers();
            }
        });
    }

    public boolean isTrusted(Player player) {
        InetSocketAddress inetSocketAddress = player.getAddress();
        if (inetSocketAddress == null) return false;

        synchronized (trustedUsers) {
            for (TrustedUser user : trustedUsers) {
                if (user.getUuid().equals(player.getUniqueId()) && player.getAddress().getHostString().equals(user.getIp())) {
                    return true;
                }
            }
        }
        return false;
    }

}
