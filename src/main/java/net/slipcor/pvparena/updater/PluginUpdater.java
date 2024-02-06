package net.slipcor.pvparena.updater;

import com.google.gson.JsonObject;
import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.core.Language;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

import static net.slipcor.pvparena.core.Language.MSG;
import static net.slipcor.pvparena.core.VersionUtils.isSameVersionOrNewer;

/**
 * Manage plugin versions and updates
 */
public class PluginUpdater extends AbstractUpdater {

    private static final String API_URL = "https://api.github.com/repos/Eredrim/pvparena/releases/latest";
    private static final String CONFIG_NODE = "update.plugin";
    private final File pluginJarFile;

    /**
     * Construct a plugin updater
     * @param msgList Reference to UpdateChecker message list
     */
    public PluginUpdater(List<String> msgList, File pluginJarFile) {
        super(msgList, CONFIG_NODE);
        this.pluginJarFile = pluginJarFile;
    }

    /**
     * Run plugin updaters : checks version and downloads update according to config
     * @throws IOException Exception if can't connect to github API
     */
    protected void runUpdater() throws IOException {
        String currentVersion = PVPArena.getInstance().getDescription().getVersion().replace("v", "");
        URL githubApi = new URL(API_URL);
        URLConnection connection = githubApi.openConnection();
        JsonObject versionJson = getVersionJson(connection.getInputStream());
        String onlineVersion = getOnlineVersionFromJson(versionJson);

        if(isSameVersionOrNewer(currentVersion, onlineVersion)) {
            LOG.info("PVP Arena is up to date");
        } else {
            String updateInfo = getAnnounceMessage(MSG.UPDATER_PLUGIN.toString(), onlineVersion, currentVersion);
            LOG.info(updateInfo);
            if(this.updateMode == UpdateMode.ANNOUNCE) {
                this.updateMsgList.add(updateInfo);
            } else if(this.updateMode == UpdateMode.DOWNLOAD ) {
                String filename = getFilenameFromJson(versionJson);
                LOG.info("Downloading update...");
                try {
                    downloadPlugin(getDownloadUrlFromJson(versionJson), filename);
                    String updateSuccess = getSuccessMessage(MSG.UPDATER_PLUGIN.toString(), onlineVersion) + " " +
                            Language.parse(MSG.UPDATER_RESTART);
                    LOG.info(updateSuccess);
                    this.updateMsgList.add(updateSuccess);
                    this.planPluginRenaming(filename);
                } catch (IOException e) {
                    LOG.warning("Error during plugin update");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Downloads new plugin version and put it into "upadate" folder
     * @param downloadUrlStr Download url
     * @param filename Name of file to download
     * @throws IOException Exception if download fails
     */
    private static void downloadPlugin(String downloadUrlStr, String filename) throws IOException {
        URL downloadUrl = new URL(downloadUrlStr);
        final File updateFolder = Bukkit.getServer().getUpdateFolderFile();
        if (!updateFolder.exists()) {
            updateFolder.mkdirs();
        }
        final File pluginFile = new File(updateFolder, filename);
        if (pluginFile.exists()) {
            pluginFile.delete();
        }
        ReadableByteChannel readableByteChannel = Channels.newChannel(downloadUrl.openStream());
        FileOutputStream outputStream = new FileOutputStream(pluginFile);
        outputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        outputStream.close();
    }

    /**
     * Creates a runnable to rename current plugin jar with new jar name in order to spigot updater
     * could replace the plugin by the "update" folder one.
     * @param fileName name of downloaded file (currently in "update" folder)
     */
    private void planPluginRenaming(String fileName) {
        final File pluginJarFile = this.pluginJarFile;
        final String newFileName = fileName;
        this.toRunOnDisable = new Runnable() {
            @Override
            public void run() {
                String currentFileName  = pluginJarFile.getName();
                File newJarFile = new File(pluginJarFile.getPath().replace(currentFileName, newFileName));
                if(!newJarFile.exists()) {
                    LOG.info("Renaming PVP Arena jar file. It will be replaced by the update on the next startup.");
                    pluginJarFile.renameTo(newJarFile);
                }
            }
        };
    }
}
