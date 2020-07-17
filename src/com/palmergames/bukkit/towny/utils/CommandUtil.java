package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.command.InviteCommand;
import com.palmergames.bukkit.towny.command.NationCommand;
import com.palmergames.bukkit.towny.command.PlotCommand;
import com.palmergames.bukkit.towny.command.ResidentCommand;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.command.TownyAdminCommand;
import com.palmergames.bukkit.towny.command.TownyCommand;
import com.palmergames.bukkit.towny.command.TownyWorldCommand;
import com.palmergames.bukkit.towny.command.commandobjects.AcceptCommand;
import com.palmergames.bukkit.towny.command.commandobjects.CancelCommand;
import com.palmergames.bukkit.towny.command.commandobjects.ConfirmCommand;
import com.palmergames.bukkit.towny.command.commandobjects.DenyCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.TabCompleter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CommandUtil {
	
	public static void implementCommands() {

		Towny plugin = Towny.getPlugin();
		
		// Only do this when the plugin is initially enabling.
		if (!plugin.isEnabled()) {
			registerSpecialCommands();
		}

		// Setup bukkit command interfaces
		Objects.requireNonNull(plugin.getCommand("townyadmin")).setExecutor(new TownyAdminCommand(plugin));
		Objects.requireNonNull(plugin.getCommand("townyworld")).setExecutor(new TownyWorldCommand(plugin));
		Objects.requireNonNull(plugin.getCommand("resident")).setExecutor(new ResidentCommand(plugin));
		Objects.requireNonNull(plugin.getCommand("towny")).setExecutor(new TownyCommand(plugin));

		CommandExecutor townCommandExecutor = new TownCommand(plugin);
		Objects.requireNonNull(plugin.getCommand("town")).setExecutor(townCommandExecutor);

		// This is needed because the vanilla "/t" tab completer needs to be overridden.
		Objects.requireNonNull(plugin.getCommand("t")).setTabCompleter((TabCompleter)townCommandExecutor);

		Objects.requireNonNull(plugin.getCommand("nation")).setExecutor(new NationCommand(plugin));
		Objects.requireNonNull(plugin.getCommand("plot")).setExecutor(new PlotCommand(plugin));
		Objects.requireNonNull(plugin.getCommand("invite")).setExecutor(new InviteCommand(plugin));
	}
	
	private static void registerSpecialCommands() {
		List<Command> commands = new ArrayList<>(4);
		commands.add(new AcceptCommand(TownySettings.getAcceptCommand()));
		commands.add(new DenyCommand(TownySettings.getDenyCommand()));
		commands.add(new ConfirmCommand(TownySettings.getConfirmCommand()));
		commands.add(new CancelCommand(TownySettings.getCancelCommand()));
		try {
			final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

			bukkitCommandMap.setAccessible(true);
			CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

			commandMap.registerAll("towny", commands);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
}
