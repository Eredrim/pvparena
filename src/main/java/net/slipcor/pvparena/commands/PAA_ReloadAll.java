package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.GlobalClasses;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.RegionManager;
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
        if (!this.hasPerms(sender)) {
            return;
        }

        if (!argCountValid(sender, args, new Integer[]{0, 1})) {
            return;
        }

        PVPArena paInstance = PVPArena.getInstance();
        paInstance.reloadConfig();

        final FileConfiguration config = paInstance.getConfig();
        Language.init(config.getString("language", "en"));

        if (args.length > 1 && args[1].equalsIgnoreCase("ymls")) {
            Arena.pmsg(sender, Language.MSG.LANG_RELOAD_DONE);
            return;
        }

        GlobalClasses.getInstance().load(); // reload classes.yml
        ArenaManager.reloadAllArenas();
        RegionManager.getInstance().reloadCache();
        Arena.pmsg(sender, Language.MSG.CMD_RELOADALL_DONE);
    }

    @Override
    public boolean hasVersionForArena() {
        return true;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
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
