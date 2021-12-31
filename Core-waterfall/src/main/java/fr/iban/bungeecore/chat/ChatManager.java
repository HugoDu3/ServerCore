package fr.iban.bungeecore.chat;

import java.util.UUID;

import fr.iban.bungeecore.CoreBungeePlugin;
import fr.iban.bungeecore.commands.StaffChatToggle;
import fr.iban.bungeecore.utils.HexColor;
import fr.iban.common.data.Account;
import fr.iban.common.data.AccountProvider;
import fr.iban.common.data.Option;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class ChatManager {

	private CoreBungeePlugin plugin;
	private boolean isMuted = false;
	private ChatColor pingColor;

	public ChatManager(CoreBungeePlugin plugin) {
		this.plugin = plugin;
		this.pingColor = ChatColor.of(plugin.getConfiguration().getString("ping-color"));
	}

	public void sendGlobalMessage(UUID uuid, String message) {
		ProxyServer server = ProxyServer.getInstance();
		server.getScheduler().runAsync(plugin, () -> {
			ProxiedPlayer player = server.getPlayer(uuid);
			String msg = message;

			if(player.hasPermission("servercore.colors")) {
				msg = HexColor.translateColorCodes(msg);
			}

			//vérif si le message est un message staff
			if(msg.startsWith("$") && player.hasPermission("servercore.staffchat")) {
				sendStaffMessage(player, msg.substring(1));
				return;
			}

			if(isMuted && !player.hasPermission("servercore.chatmanage")) {
				return;
			}

			AccountProvider ap = new AccountProvider(uuid);
			Account account = ap.getAccount();

			String prefix =  plugin.getConfiguration().getString("chat-format");
			prefix = prefix.replace("%player%", player.getName());
			prefix = prefix.replace("%lp_prefix%", getPrefix(player));
			prefix = prefix.replace("%lp_suffix%", getSuffix(player));
			prefix = prefix.replace("%premium%", getPremiumString(player));
			prefix = HexColor.translateColorCodes(prefix);

			msg = prefix+msg;

			if(!account.getOption(Option.TCHAT)) {
				player.sendMessage(TextComponent.fromLegacyText("§cVous ne pouvez pas envoyer ce message car votre tchat est désactivé"));
				ProxyServer.getInstance().getLogger().info(translateColors(HexColor.translateColorCodes("§8[§CDÉSACTIVÉ§8]§r " + msg)));
				return;
			}

			//Envoi du message à chaque joueur
			for (ProxiedPlayer p: ProxyServer.getInstance().getPlayers()) {
				String pmessage = msg;
				Account account2 = new AccountProvider(p.getUniqueId()).getAccount();
				if(!account2.getOption(Option.TCHAT)) continue;

				//vérif si le joueur est mentionné dans le message.
				if (!p.getUniqueId().equals(player.getUniqueId()) && pmessage.toLowerCase().contains(p.getName().toLowerCase()) && account2.getOption(Option.MENTION)) {
					String ping = pingColor + "@" + p.getName() + "§r";
					pmessage = pmessage.replace(p.getName(), ping);
				}

				if(!account2.getIgnoredPlayers().contains(player.getUniqueId())){
					p.sendMessage(TextComponent.fromLegacyText(pmessage));
				}

				new ComponentBuilder().append(TextComponent.fromLegacyText(pmessage)).create();
			}

			//Envoi du message à la console
			ProxyServer.getInstance().getLogger().info(msg);

		});
	}

	public void sendAnnonce(UUID uuid, String annonce) {
		ProxyServer server = ProxyServer.getInstance();
		ProxiedPlayer player = server.getPlayer(uuid);
		String message = annonce;

		if(isMuted && !player.hasPermission("servercore.chatmanage")) {
			return;
		}

		ProxyServer.getInstance().broadcast(TextComponent.fromLegacyText(HexColor.translateColorCodes("#f07e71§lAnnonce de #fbb29e§l"+ player.getName() + " #f07e71➤ #7bc8fe§l" + message)));
	}

	public void sendRankup(UUID uuid, String group) {
		ProxyServer server = ProxyServer.getInstance();
		ProxiedPlayer player = server.getPlayer(uuid);

		ProxyServer.getInstance().broadcast(TextComponent.fromLegacyText(HexColor.FLAT_PINK.getColor() + player.getName() + " a été promu " + group + "!"));

	}

	public boolean isMuted() {
		return isMuted;
	}

	public void setMuted(boolean isMuted) {
		this.isMuted = isMuted;
	}

	private String translateColors(String string) {
		return ChatColor.translateAlternateColorCodes('&', string);
	}

	private void sendStaffMessage(ProxiedPlayer sender, String message) {
		ProxyServer.getInstance().getPlayers().forEach( p -> {
			if(p.hasPermission("servercore.staffchat")) {
				if (!StaffChatToggle.sc.contains(p)) {
					p.sendMessage(TextComponent.fromLegacyText(HexColor.translateColorCodes("§8[§3§lStaff§8] " + getSuffix(sender) + "§l" + getPrefix(sender) + "§l " + sender.getName() + " §8§l➤ " + getSuffix(sender) + "§l" + message)));
				}
			}
		});
		ProxyServer.getInstance().getLogger().info(HexColor.translateColorCodes("§8[§3§lStaff§8] " + getSuffix(sender) + "§l" + getPrefix(sender) + "§l " + sender.getName() + " §8§l➤ " + getSuffix(sender) + "§l" + message));
	}

	public void toggleChat(CommandSender sender) {
		isMuted = !isMuted;
		if(isMuted) {
			ProxyServer.getInstance().broadcast(TextComponent.fromLegacyText("§cLe chat a été rendu muet par "+ sender.getName()+"."));
		}else {
			ProxyServer.getInstance().broadcast(TextComponent.fromLegacyText("§aLe chat n'est plus muet."));
		}
	}

	/*
	 * Luckperms
	 */

	private LuckPerms luckapi = LuckPermsProvider.get();

	private User loadUser(ProxiedPlayer player) {
		if (!player.isConnected())
			throw new IllegalStateException("Player is offline!");
		return luckapi.getUserManager().getUser(player.getUniqueId());
	}

	private CachedMetaData playerMeta(ProxiedPlayer player) {
		return loadUser(player).getCachedData().getMetaData(luckapi.getContextManager().getQueryOptions(player));
	}

	private String getPrefix(ProxiedPlayer player) {
		String prefix = playerMeta(player).getPrefix();
		return (prefix != null) ? prefix : "";
	}

	private String getSuffix(ProxiedPlayer player) {
		String suffix = playerMeta(player).getSuffix();
		return (suffix != null) ? suffix : "";
	}

	private String getPremiumString(ProxiedPlayer player){
		if(player.hasPermission("premium")){
			return "✮ ";
		}else{
			return "";
		}
	}

}