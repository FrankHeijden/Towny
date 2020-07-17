package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.utils.CommandUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

public class TownyServerListener implements Listener {
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onServerLoad(ServerLoadEvent event) {
		// Override any of the commands by other plugins.
		CommandUtil.implementCommands();
	}
}
