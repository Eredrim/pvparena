package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.managers.ArenaManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena DEBUG Command class</pre>
 * <p/>
 * A command to toggle debugging
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAA_ReloadAll extends AbstractGlobalCommand {

    public PAA_ReloadAll() {
        super(new String[]{"pvparena.cmds.reload"});
    }

    @Override
    public void commit(final CommandSender sender, final String[] args) {
        if (!hasPerms(sender)) {
            return;
        }

        if (!argCountValid(sender, args, new Integer[]{0, 1})) {
            return;
        }

        final PAA_Reload scmd = new PAA_Reload();

        PVPArena.instance.reloadConfig();

        final FileConfiguration config = PVPArena.instance.getConfig();
        Language.init(config.getString("language", "en"));
        Help.init(config.getString("language", "en"));

        if (args.length > 1 && args[1].equalsIgnoreCase("ymls")) {
            Arena.pmsg(sender, Language.parse(Language.MSG.RELOAD_YMLS_DONE));
            return;
        }

        final String[] emptyArray = new String[0];

        for (Arena a : ArenaManager.getArenas()) {
            scmd.commit(a, sender, emptyArray);
        }

        ArenaClass.addGlobalClasses(); // reload classes.yml
        ArenaManager.load_arenas();
        if (config.getBoolean("use_shortcuts") || config.getBoolean("only_shortcuts")) {
            ArenaManager.readShortcuts(config.getConfigurationSection("shortcuts"));
        }
    }

    @Override
    public boolean hasVersionForArena() {
        return true;
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, Help.parse(HELP.DEBUG));
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("reload");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!rl");
    }

    @Override
    public CommandTree<String> getSubs(final Arena nothing) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"ymls"});
        return result;
    }
}
