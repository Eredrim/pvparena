package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Collections;
import java.util.List;

/**
 * <pre>PVP Arena DUTY Command class</pre>
 * <p/>
 * A command to toggle the override permission
 *
 * @author slipcor
 */

public class PAA_Duty extends AbstractGlobalCommand {

    public PAA_Duty() {
        super(new String[]{"pvparena.cmds.duty"});
    }

    @Override
    public void commit(final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender)) {
            return;
        }

        if (!argCountValid(sender, args, new Integer[]{0})) {
            return;
        }

        boolean perm = true;

        for (PermissionAttachmentInfo pai : sender.getEffectivePermissions()) {
            if ("pvparena.override".equals(pai.getPermission())) {
                perm = !pai.getValue();
                /*sender.removeAttachment(pai.getAttachment());*/
                break;
            }
        }

        sender.addAttachment(PVPArena.getInstance(), "pvparena.override", perm);
        sender.recalculatePermissions();

        if (perm) {
            Arena.pmsg(sender, MSG.DUTY_TRUE);
        } else {
            Arena.pmsg(sender, MSG.DUTY_FALSE);
        }
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.DUTY);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("duty");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!du");
    }

    @Override
    public CommandTree<String> getSubs(final Arena nothing) {
        return new CommandTree<>(null);
    }
}
