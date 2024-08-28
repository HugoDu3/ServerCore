package fr.iban.survivalcore.listeners;

import com.github.puregero.multilib.MultiLib;
import com.github.puregero.multilib.regionized.RegionizedScheduler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.PortalCreateEvent.CreateReason;
import org.bukkit.scheduler.BukkitRunnable;

import fr.iban.survivalcore.SurvivalCorePlugin;

public class PortalListeners implements Listener {

	
	@EventHandler
	public void onPortalCreate(PortalCreateEvent e) {
		if(e.getReason() == CreateReason.FIRE && e.getWorld().getEnvironment() == Environment.NETHER) {
			if(!e.getBlocks().isEmpty() && e.getBlocks().get(0).getLocation().getY() >= 128) {
				for (BlockState block : e.getBlocks()) {
					if (block.getType() == Material.NETHER_PORTAL) {
						Location blockLocation = block.getLocation();
						RegionizedScheduler regionScheduler = MultiLib.getRegionScheduler();
						regionScheduler.runDelayed(SurvivalCorePlugin.getInstance(), blockLocation, task -> {
							block.getBlock().breakNaturally();
						}, 6000);
						break;
					}
				}
			}
		}
	}
}
