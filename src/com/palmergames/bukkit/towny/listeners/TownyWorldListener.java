package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyWorld;
import java.util.ArrayList;
import java.util.List;

import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class TownyWorldListener implements Listener {
	
	private final Towny plugin;

	public TownyWorldListener(Towny instance) {

		plugin = instance;
	}
	
	public static List<String> playersMap = new ArrayList<String>();

	@EventHandler(priority = EventPriority.NORMAL)
	public void onWorldLoad(WorldLoadEvent event) {

		newWorld(event.getWorld().getName());
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onWorldInit(WorldInitEvent event) {

		newWorld(event.getWorld().getName());

	}

	private void newWorld(String worldName) {
		
		boolean dungeonWorld = false;
		
		// Don't create a new world for temporary DungeonsXL instanced worlds.
		if (Bukkit.getServer().getPluginManager().getPlugin("DungeonsXL") != null)
			if (worldName.startsWith("DXL_")) {
				dungeonWorld = true;
			}
				

		//String worldName = event.getWorld().getName();
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		try {
			townyUniverse.getDataSource().newWorld(worldName);
			TownyWorld world = townyUniverse.getDataSource().getWorld(worldName);
			if (dungeonWorld)
				world.setUsingTowny(false);
			
			if (world == null)
				TownyMessaging.sendErrorMsg("Could not create data for " + worldName);
			else {
				if (!dungeonWorld)
					if (!townyUniverse.getDataSource().loadWorld(world)) {
						// First time world has been noticed
						townyUniverse.getDataSource().saveWorld(world);
					}
			}
		} catch (AlreadyRegisteredException e) {
			// Allready loaded			
		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg("Could not create data for " + worldName);
			e.printStackTrace();
		}
	}
	
	/**
	 * Protect trees and mushroom growth transforming neighbouring plots which do not share the same owner. 
	 * @param event - StructureGrowEvent
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onStructureGrow(StructureGrowEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getWorld()))
			return;

		TownBlock townBlock = null;
		TownBlock otherTownBlock = null;
		Town town = null;
		Town otherTown = null;
		Resident resident = null;
		TownyWorld world = null;
		List<BlockState> removed = new ArrayList<>();
		try {
			world = TownyUniverse.getInstance().getDataSource().getWorld(event.getWorld().getName());
		} catch (NotRegisteredException e) {
			return;
		} 
		// The event Location is always one spot, and although 2x2 trees technically should have 4 locations, 
		// we can trust that the saplings were all placed by one person, or group of people, who were allowed
		// to place them.
		Coord coord = Coord.parseCoord(event.getLocation());
		for (BlockState blockState : event.getBlocks()) {
			Coord blockCoord = Coord.parseCoord(blockState.getLocation());
			// Wilderness so continue.
			if (!world.hasTownBlock(blockCoord)) {
				continue;
			}

			// Same townblock as event location, continue;
			if (coord.equals(blockCoord)) {
				continue;
			}
			if (world.hasTownBlock(coord)) {
				townBlock = TownyAPI.getInstance().getTownBlock(event.getLocation());
				// Resident Owned Location
				if (townBlock.hasResident()) {
					try {
						resident = townBlock.getResident();
					} catch (NotRegisteredException e) {
					}
					otherTownBlock = TownyAPI.getInstance().getTownBlock(blockState.getLocation());
					try {
						// if residents don't match.
						if (otherTownBlock.hasResident() && otherTownBlock.getResident() != resident) {
							removed.add(blockState);
							continue;
						// if plot doesn't have a resident.
						} else if (!otherTownBlock.hasResident()) {
							removed.add(blockState);
							continue;
						// if both townblock have same owner. 
						} else if (resident == otherTownBlock.getResident()) {
							continue;
						}
					} catch (NotRegisteredException e) {
					}
				// Town Owned Location
				} else {
					try {
						town = townBlock.getTown();
					} catch (NotRegisteredException e) {
					}
					try {
						otherTownBlock = TownyAPI.getInstance().getTownBlock(blockState.getLocation());
						otherTown = otherTownBlock.getTown();
					} catch (NotRegisteredException e) {
					}
					// If towns don't match.
					if (town != otherTown) {						
						removed.add(blockState);
						continue;
					// If town-owned is growing into a resident-owned plot.
					} else if (otherTownBlock.hasResident()) {
						removed.add(blockState);
						continue;
					// If towns match.
					} else if (town == otherTown) {
						continue;
					}
				}
			} else {
				// Growth in wilderness	affecting blockState in town.
				removed.add(blockState);
				continue;
			}	
		}
		if (!removed.isEmpty())
			event.getBlocks().removeAll(removed);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPortalCreate(PortalCreateEvent event) {
		if (!(event.getReason() == PortalCreateEvent.CreateReason.NETHER_PAIR)) {
			return;
		}
		try {
			if (!TownyUniverse.getInstance().getDataSource().getWorld(event.getWorld().getName()).isUsingTowny()) {
				return;
			}
		} catch (Exception ignored) {}

		if (!event.getEntity().getType().equals(EntityType.PLAYER)) {
			return;
		}
		
		for (BlockState block : event.getBlocks()) {
			// Check if player can build in destination portal townblock.
			boolean bBuild = PlayerCacheUtil.getCachePermission((Player) event.getEntity(), block.getLocation(), Material.OBSIDIAN, TownyPermission.ActionType.BUILD);

			// If not reject the creation of the portal. No need to cancel event, bukkit does that automatically.
			if (!bBuild) {
				TownyMessaging.sendErrorMsg(event.getEntity(), TownySettings.getLangString("msg_err_you_are_not_allowed_to_create_the_other_side_of_this_portal"));
				event.setCancelled(true);
				break;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerPortalEvent(PlayerPortalEvent event) {

		// Check if this is caused by portal enter or not.
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL && event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
			return;
		}

		Player player = event.getPlayer();

		if (player.isOp()) {
			return;
		}

		// Get the townblock the player is on.
		WorldCoord playerCoord = WorldCoord.parseWorldCoord(player.getLocation());

		TownBlock townBlock;
		Town town;
		Resident resident;
		try {
			townBlock = playerCoord.getTownBlock();
			town = townBlock.getTown();
			resident = TownyUniverse.getInstance().getDataSource().getResident(player.getName());
		} catch (NotRegisteredException e) {
			return;
		}

		if (town.hasResident(resident) && !townBlock.getPermissions().getResidentPerm(TownyPermission.ActionType.ITEM_USE)) {
			// Send error message.
			TownyMessaging.sendErrorMsg(player, "Residents can't itemuse.");
			return;
		}

		if (!townBlock.getPermissions().getOutsiderPerm(TownyPermission.ActionType.ITEM_USE)) {
			// Send error message.
			TownyMessaging.sendErrorMsg(player, "Outsiders can't itemuse.");
			return;
		}

		// This player is not allowed to use the portal.
		event.setCancelled(true);


	}

}
