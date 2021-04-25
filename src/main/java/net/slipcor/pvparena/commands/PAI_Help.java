package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena INFO Command class</pre>
 * <p/>
 * A command to display the active modules of an arena and settings
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAI_Help extends AbstractGlobalCommand {

    public PAI_Help() {
        super(new String[]{"pvparena.user", "pvparena.cmds.help"});
    }

    @Override
    public void commit(final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender)) {
            return;
        }

        if (!argCountValid(sender, args, new Integer[]{0, 1})) {
            return;
        }
        /*
            /pvparena help
            /pvparena help admin | help administrating
            /pvparena help setup | help setting up
            /pvparena help custom | help customizing
            /pvparena help game | help ingame
            /pvparena help info | help getting information
		 */

        if (args.length > 0) {
            if ("admin".equals(args[0])) {
                Arena.pmsg(sender, MSG.HELP_HEADLINE, "admin");
                Arena.pmsg(sender, "/pa help check");
                Arena.pmsg(sender, "/pa help debug");
                Arena.pmsg(sender, "/pa help disable");
                Arena.pmsg(sender, "/pa help enable");
                Arena.pmsg(sender, "/pa help reload");
                Arena.pmsg(sender, "/pa help remove");
                Arena.pmsg(sender, "/pa help stop");
                Arena.pmsg(sender, "/pa help teleport");

            } else if ("setup".equals(args[0])) {
                Arena.pmsg(sender, MSG.HELP_HEADLINE, "setup");
                Arena.pmsg(sender, "/pa help autosetup");
                Arena.pmsg(sender, "/pa help class");
                Arena.pmsg(sender, "/pa help create");
                Arena.pmsg(sender, "/pa help gamemode");
                Arena.pmsg(sender, "/pa help goal");
                Arena.pmsg(sender, "/pa help set");
                Arena.pmsg(sender, "/pa help setowner");
                Arena.pmsg(sender, "/pa help spawn");

            } else if ("custom".equals(args[0])) {
                Arena.pmsg(sender, MSG.HELP_HEADLINE, "custom");
                Arena.pmsg(sender, "/pa help blacklist");
                Arena.pmsg(sender, "/pa help edit");
                Arena.pmsg(sender, "/pa help install");
                Arena.pmsg(sender, "/pa help protection");
                Arena.pmsg(sender, "/pa help region");
                Arena.pmsg(sender, "/pa help regionflag");
                Arena.pmsg(sender, "/pa help regions");
                Arena.pmsg(sender, "/pa help regiontype");
                Arena.pmsg(sender, "/pa help round");
                Arena.pmsg(sender, "/pa help uninstall");
                Arena.pmsg(sender, "/pa help update");
                Arena.pmsg(sender, "/pa help whitelist");

            } else if ("game".equals(args[0])) {
                Arena.pmsg(sender, MSG.HELP_HEADLINE, "game");

                Arena.pmsg(sender, "/pa help chat");
                Arena.pmsg(sender, "/pa help join");
                Arena.pmsg(sender, "/pa help leave");
                Arena.pmsg(sender, "/pa help spectate");
            } else if ("info".equals(args[0])) {
                Arena.pmsg(sender, MSG.HELP_HEADLINE, "info");

                Arena.pmsg(sender, "/pa help arenalist");
                Arena.pmsg(sender, "/pa help help");
                Arena.pmsg(sender, "/pa help info");
                Arena.pmsg(sender, "/pa help list");
                Arena.pmsg(sender, "/pa help ready");
                Arena.pmsg(sender, "/pa help stats");
                Arena.pmsg(sender, "/pa help version");
            } else {
                for (AbstractArenaCommand aac : PVPArena.getInstance().getArenaCommands()) {
                    if (aac.getMain().contains(args[0]) || aac.getShort().contains(args[0])) {
                        aac.displayHelp(sender);
                        return;
                    }
                }

                if ("arenalist".equals(args[0])) {
                    args[0] = "list";
                }

                for (AbstractGlobalCommand cmd : PVPArena.getInstance().getGlobalCommands()) {
                    if (cmd.getMain().contains(args[0]) || cmd.getShort().contains(args[0])) {
                        cmd.displayHelp(sender);
                        return;
                    }
                }
            }
            return;
        }

        Arena.pmsg(sender, MSG.HELP_HEADLINE, "");
        Arena.pmsg(sender, MSG.HELP_ADMIN, "/pvparena help admin");
        Arena.pmsg(sender, MSG.HELP_SETUP, "/pvparena help setup");
        Arena.pmsg(sender, MSG.HELP_CUSTOM, "/pvparena help custom");
        Arena.pmsg(sender, MSG.HELP_GAME, "/pvparena help game");
        Arena.pmsg(sender, MSG.HELP_INFO, "/pvparena help info");
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.HELP);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("help");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("-h");
    }

    @Override
    public CommandTree<String> getSubs(final Arena nothing) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"admin"});
        result.define(new String[]{"setup"});
        result.define(new String[]{"custom"});
        result.define(new String[]{"game"});
        result.define(new String[]{"info"});
        return result;
    }
}
