package fr.iban.survivalcore.listeners;

import fr.iban.bukkitcore.CoreBukkitPlugin;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public class SpawnListeners implements Listener {
	
	@EventHandler
	public void onEntitySpawn(CreatureSpawnEvent e) {
		if(e.getSpawnReason() == SpawnReason.SPAWNER) {
			if(e.getEntityType() == EntityType.CAVE_SPIDER) {
				return;
			}

			Location location = e.getLocation();

			CoreBukkitPlugin.getInstance().runRegionTask(location, () -> {
				e.getEntity().setAI(false);
				e.getEntity().setGravity(true);
			});
		}
	}

}
