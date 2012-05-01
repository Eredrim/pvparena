package net.slipcor.pvparena.command;

import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.managers.Arenas;

import org.bukkit.command.CommandSender;

/**
 * list command class
 * 
 * @author slipcor
 * 
 * @version v0.7.18
 * 
 */

public class PAList extends PA_Command {

	@Override
	public void commit(CommandSender player, String[] args) {
		Arenas.tellPlayer(player,
				Language.parse("arenas", Arenas.getNames()));
	}

}
