package net.slipcor.pvparena.commands;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loader.Loadable;
import net.slipcor.pvparena.updater.ModulesUpdater;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <pre>
 * PVP Arena MODULES Command class
 * </pre>
 * <p/>
 * A command to manage modules (list, install, uninstall, update, download and upgrade)
 *
 * @author Eredrim
 * @version v1.15
 */

public class PAA_Modules extends AbstractGlobalCommand {

    public PAA_Modules() {
        super(new String[]{"pvparena.cmds.modules"});
    }

    private static final File FILES_DIR = new File(PVPArena.getInstance().getDataFolder(),"/files/");
    private static final File MODS_DIR = new File(PVPArena.getInstance().getDataFolder(),"/mods/");

    @Override
    public void commit(final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender)) {
            return;
        }

        if (!argCountValid(sender, args, new Integer[]{0, 1, 2})) {
            return;
        }

        // pa modules
        if (args.length == 0) {
            listModules(sender);
        } else if (args.length == 1) {
            switch (args[0]) {
                case "list":
                    listModules(sender);
                    break;
                case "update":
                    updateModules(sender);
                    break;
                case "download":
                    ModulesUpdater.downloadModulePack(sender);
                    break;
                case "upgrade":
                    ModulesUpdater.downloadModulePack(sender);
                    updateModules(sender);
                    break;
                case "install":
                case "uninstall":
                    Arena.pmsg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, "1", "2");
                    break;
                default:
                    // Show specific help
            }
        } else { // 2 args only (if more args, caught above)
            if("install".equalsIgnoreCase(args[0])) {
                installModule(sender, args[1]);
            } else if ("uninstall".equalsIgnoreCase(args[0])) {
                uninstallModule(sender, args[1]);
            } else {
                Arena.pmsg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, "2", "1");
            }
        }
    }

    /**
     * List all modules status to sender
     * @param sender User who typed the command
     */
    private static void listModules(final CommandSender sender) {
        Arena.pmsg(sender, "--- PVP Arena Version Update information ---");
        Arena.pmsg(sender, "[" + ChatColor.GRAY + "uninstalled" + ChatColor.RESET + " | " + ChatColor.YELLOW + "installed" + ChatColor.RESET + "]");
        Arena.pmsg(sender, ChatColor.GREEN + "--- Installed Arena Mods ---->");

        for (String modName : getModInFilesFolder()) {
            final boolean modLoaded = PVPArena.getInstance().getAmm().hasLoadable(modName);
            Arena.pmsg(sender, (modLoaded ? ChatColor.YELLOW : ChatColor.GRAY) + modName + ChatColor.RESET);
        }
    }

    /**
     * Install a module (copying from /files to /mods)
     * @param sender User who typed the command
     * @param name Module named typed by user
     */
    private static void installModule(CommandSender sender, String name) {
        Set<String> modList = getModInFilesFolder();
        String modName = name.toLowerCase();
        if (modList.size() != 0 && modList.contains(modName)) {

            if (copyFile("pa_m_" + modName + ".jar")) {
                PVPArena.getInstance().getAmm().reload();
                Arena.pmsg(sender, MSG.INSTALL_DONE, modName);
            } else {
                Arena.pmsg(sender, MSG.ERROR_INSTALL, modName);
            }
        } else {
            Arena.pmsg(sender, MSG.ERROR_UNKNOWN_MODULE, name);
        }
    }

    /**
     * Uninstall a module
     * Unload it and remove it from /mods directory
     * @param sender User who typed the command
     * @param name Module named typed by user
     */
    private static void uninstallModule(CommandSender sender, String name) {
        final boolean modLoaded = PVPArena.getInstance().getAmm().hasLoadable(name);
        if (modLoaded) {
            String modName = name;
            String jarName = "pa_m_" + modName.toLowerCase() + ".jar";
            if (remove(jarName)) {
                PVPArena.getInstance().getAmm().reload();
                Arena.pmsg(sender, MSG.UNINSTALL_DONE, modName);
            } else {
                Arena.pmsg(sender, MSG.ERROR_UNINSTALL, modName);
                FileConfiguration cfg = PVPArena.getInstance().getConfig();
                List<String> toDelete = cfg.getStringList("todelete");
                toDelete.add(jarName);
                cfg.set("todelete", toDelete);
                PVPArena.getInstance().saveConfig();
                Arena.pmsg(sender, MSG.ERROR_UNINSTALL2);
            }
        }
    }

    /**
     * Replace all modules of /mods directory by ones of /files directory
     * @param sender User who typed the command
     */
    private static void updateModules(CommandSender sender) {
        PVPArena.getInstance().getAmm().getAllLoadables().stream()
                .filter(loadable -> !loadable.isInternal())
                .forEach(loadable -> {
                    final File jarFile = new File(FILES_DIR, "pa_m_" + loadable.getName().toLowerCase() + ".jar");

                    if (jarFile.exists()) {
                        uninstallModule(sender, loadable.getName());
                        installModule(sender, loadable.getName());
                    }
                });
    }

    /**
     * List all mods in /files folder
     * @return List of mod names
     */
    private static Set<String> getModInFilesFolder() {
        Set<String> modList = new HashSet<>();
        for (File file : FILES_DIR.listFiles()) {
            final String fileName = file.getName();
            if (fileName.startsWith("pa_m_") && fileName.endsWith(".jar")) {
                String modName = fileName.substring(5, fileName.length() - 4);
                modList.add(modName);
            }
        }
        return modList;
    }

    /**
     * Copy a file from /files folder to /mod folder
     * @param file file name
     * @return true if install was successful
     */
    private static boolean copyFile(final String file) {

        final File source = new File(FILES_DIR, file);

        if (!source.exists()) {
            Arena.pmsg(
                    Bukkit.getConsoleSender(),
                    String.format("%sFile '%s%s%s' not found. Please extract the file to /files before trying to install!", ChatColor.RED, ChatColor.RESET, file, ChatColor.RED));
            return false;
        }

        try {
            final File destination = new File(MODS_DIR, file);
            final FileInputStream stream = new FileInputStream(source);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) > 0) {
                baos.write(buffer, 0, bytesRead);
            }

            final FileOutputStream fos = new FileOutputStream(destination);
            fos.write(baos.toByteArray());
            fos.close();

            PVPArena.getInstance().getLogger().info("Installed module " + file);
            stream.close();
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Remove a mod from /mods folder
     * @param file module file name
     * @return true if deletion was successful
     */
    public static boolean remove(final String file) {
        if (!MODS_DIR.exists()) {
            PVPArena.getInstance().getLogger().severe("unable to fetch file: " + file);
            return false;
        }

        final File destFile = new File(MODS_DIR, file);

        boolean exists = destFile.exists();
        boolean deleted = false;
        if (exists) {
            deleted = destFile.delete();
            if (!deleted) {
                PVPArena.getInstance().getLogger().severe("could not delete file: " + file);
            }
        } else {
            PVPArena.getInstance().getLogger().warning("file does not exist: " + file);
        }

        return exists && deleted;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.INSTALL);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("modules");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!m");
    }

    @Override
    public CommandTree<String> getSubs(final Arena nothing) {
        final CommandTree<String> result = new CommandTree<>(null);

        result.define(new String[]{"download"});
        result.define(new String[]{"list"});
        result.define(new String[]{"update"});
        result.define(new String[]{"upgrade"});

        getModInFilesFolder().forEach(modName -> result.define(new String[]{"install", modName}));

        PVPArena.getInstance().getAmm().getAllLoadables().stream()
                .filter(loadable -> !loadable.isInternal())
                .map(Loadable::getName)
                .forEach(modName -> result.define(new String[]{"uninstall", modName}));

        return result;
    }
}
