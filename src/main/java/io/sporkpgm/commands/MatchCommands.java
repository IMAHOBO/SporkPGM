package io.sporkpgm.commands;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import io.sporkpgm.Spork;
import io.sporkpgm.map.SporkMap;
import io.sporkpgm.match.Match;
import io.sporkpgm.match.MatchPhase;
import io.sporkpgm.player.SporkPlayer;
import io.sporkpgm.player.event.PlayerChatEvent;
import io.sporkpgm.rotation.RotationSlot;
import io.sporkpgm.team.SporkTeam;
import io.sporkpgm.util.StringUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MatchCommands {

	@Command(aliases = {"join"}, desc = "Join the match", usage = "[team]", max = 1)
	public static void join(CommandContext cmd, CommandSender sender) throws CommandException {
		if(!(sender instanceof Player))
			throw new CommandException("Only Players can use this command");

		SporkMap map = RotationSlot.getRotation().getCurrent();
		SporkPlayer player = SporkPlayer.getPlayer((Player) sender);
		if(cmd.argsLength() == 1) {
			List<SporkTeam> teams = map.getTeams(cmd.getString(0));
			if(teams.size() > 1) {
				List<String> names = new ArrayList<>();
				for(SporkTeam team : teams) {
					names.add(team.getColoredName());
				}
				sender.sendMessage(ChatColor.RED + "Too many teams matched that query!");
				sender.sendMessage(ChatColor.GRAY + "Returned: " + StringUtil.listToEnglishCompound(names, "", ChatColor.GRAY.toString()));
				return;
			} else if(teams.size() == 0) {
				throw new CommandException("No teams matched query");
			}

			SporkTeam team = teams.get(0);
			/*
			if(!team.isObservers()) {
				if(!player.hasPermission("spork.team.select")) {
					throw new CommandPermissionsException();
				}
			}
			*/

			if(!team.canJoin(player)) {
				team.reasonJoin(player, ChatColor.GRAY);
				return;
			}

			player.setTeam(team);
			return;
		}

		SporkTeam lowest = map.getLowestTeam();
		if(!lowest.canJoin(player)) {
			player.getPlayer().sendMessage(lowest.reasonJoin(player, ChatColor.GRAY));
			return;
		}

		player.setTeam(lowest);
	}

	@Command(aliases = {"match"}, desc = "View match information")
	public static void match(CommandContext cmd, CommandSender sender) throws CommandException {
		if(!(sender instanceof Player)) {
			throw new CommandException("Only Players can use this command");
		}

		Match match = Spork.get().getMatch();
		sender.sendMessage(ChatColor.GOLD + "Match Information");
		sender.sendMessage(ChatColor.DARK_AQUA + "Map playing: " + match.getMap().getName());
		sender.sendMessage(ChatColor.DARK_AQUA + "Status: " + match.getPhase().toString());
		sender.sendMessage(ChatColor.DARK_AQUA + "Match time: " + match.getMatchTime());
	}

	@Command(aliases = {"g", "!", "all"}, desc = "Global chat", usage = "[message]", min = 1)
	public static void global(CommandContext cmd, CommandSender sender) throws CommandException {
		if(!(sender instanceof Player)) {
			throw new CommandException("Only player can use global chat");
		}

		SporkPlayer player = SporkPlayer.getPlayer((Player) sender);
		PlayerChatEvent pce = new PlayerChatEvent(player, cmd.getJoinedStrings(0), false);
		Spork.callEvent(pce);
	}

	@Command(aliases = {"start"}, desc = "Start the map with the specified countdown", usage = "[time]", min = 1, max = 1)
	@CommandPermissions("spork.match.start")
	public static void start(CommandContext cmd, CommandSender sender) throws CommandException {
		Match match = RotationSlot.getRotation().getCurrentMatch();
		MatchPhase phase = match.getPhase();
		if(match.getMap().getPlayers().size() > 1) {
			if(phase == MatchPhase.WAITING) {
				match.setPhase(MatchPhase.STARTING, true);
			}

			if(phase == MatchPhase.STARTING) {
				match.setDuration(cmd.getInteger(0));
				match.start();
				return;
			}
			sender.sendMessage(ChatColor.RED + "Server must be waiting or starting to set start time");
		} else {
			sender.sendMessage(ChatColor.RED + "At least two players need to be in the game to start it!");
		}
	}

	@Command(aliases = {"cancel", "end"}, desc = "Cancel any timers (Starting, Match, Cycling)", max = 0)
	@CommandPermissions("spork.match.cancel")
	public static void cancel(CommandContext cmd, CommandSender sender) throws CommandException {
		Match match = RotationSlot.getRotation().getCurrentMatch();
		MatchPhase phase = match.getPhase();

		if(phase == MatchPhase.PLAYING) {
			match.getMap().setEnded(true);
			return;
		}

		if(phase == MatchPhase.STARTING || phase == MatchPhase.CYCLING) {
			match.stop();
			return;
		}

		sender.sendMessage(ChatColor.RED + "Server must be starting, playing or cycling to cancel the countdown");
	}

	@Command(aliases = {"cycle"}, desc = "Cycle the map with the specified countdown", usage = "[time]", min = 1, max = 1)
	@CommandPermissions("spork.match.cycle")
	public static void cycle(CommandContext cmd, CommandSender sender) throws CommandException {
		Match match = RotationSlot.getRotation().getCurrentMatch();
		MatchPhase phase = match.getPhase();

		if(phase == MatchPhase.PLAYING) {
			match.getMap().setEnded(true);
		}

		if(phase == MatchPhase.CYCLING) {
			match.setDuration(cmd.getInteger(0));
			match.start();
			return;
		}

		sender.sendMessage(ChatColor.RED + "Server must be playing or cycling to set cycle time");
	}

}