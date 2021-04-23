package net.slipcor.pvparena.core;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.loadables.ArenaRegionShape;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionFlag;
import net.slipcor.pvparena.regions.RegionProtection;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.core.ItemStackUtils.getItemStacksFromConfig;
import static net.slipcor.pvparena.core.Utils.getSerializableItemStacks;

/**
 * <pre>
 * Configuration class
 * </pre>
 * <p/>
 * This Config wrapper improves access to config files by storing them in RAM
 * and providing quick, secured, access. Thanks a lot to garbagemule for the
 * start of this config.
 *
 * @author slipcor
 */

public class Config {
    private final YamlConfiguration cfg;
    private final File configFile;
    private final Map<String, Boolean> booleans;
    private final Map<String, Integer> ints;
    private final Map<String, Double> doubles;
    private final Map<String, String> strings;

    public enum CFG {


        Z("configversion", "v0.9.0.0", null),

        CHAT_COLORNICK("chat.colorNick", true, null),
        CHAT_DEFAULTTEAM("chat.defaultTeam", false, null),
        CHAT_ENABLED("chat.enabled", true, null),
        CHAT_ONLYPRIVATE("chat.onlyPrivate", false, null),
        CHAT_TOGLOBAL("chat.toGlobal", "none", null),

        CMDS_DEFAULTJOIN("cmds.defaultjoin", true, null),

        DAMAGE_ARMOR("damage.armor", true, null),
        DAMAGE_BLOODPARTICLES("damage.bloodParticles", true, null),
        DAMAGE_FROMOUTSIDERS("damage.fromOutsiders", false, null),
        DAMAGE_SPAWNCAMP("damage.spawncamp", 1, null),
        DAMAGE_WEAPONS("damage.weapons", true, null),

        GENERAL_CLASSSPAWN("general.classspawn", false, null),
        GENERAL_CLASSSWITCH_AFTER_RESPAWN("general.classSwitchAfterRespawn", false, null),
        GENERAL_CUSTOMRETURNSGEAR("general.customReturnsGear", false, null),
        GENERAL_ENABLED("general.enabled", true, null),
        GENERAL_GAMEMODE("general.gamemode", GameMode.SURVIVAL, null),
        GENERAL_LEAVEDEATH("general.leavedeath", false, null),
        GENERAL_LANG("general.lang", "none", null),
        GENERAL_OWNER("general.owner", "server", null),
        GENERAL_REGIONCLEAREXCEPTIONS("general.regionclearexceptions", new ArrayList<String>(), null),
        GENERAL_QUICKSPAWN("general.quickspawn", true, null),
        GENERAL_PREFIX("general.prefix", "PVP Arena", null),
        GENERAL_SHOWREMAININGLIVES("general.showRemainingLives", true, null),
        GENERAL_SMARTSPAWN("general.smartspawn", false, null),
        GENERAL_TIMER("general.timer.end", 0, null),
        GENERAL_TIMER_WINNER("general.timer.winner", "none", null),
        GENERAL_TYPE("general.type", "none", null),
        GENERAL_GOAL("general.goal", "none", null),
        GENERAL_ADDLIVESPERPLAYER("general.addLivesPerPlayer", false, null),

        ITEMS_EXCLUDEFROMDROPS("items.excludeFromDrops", new ItemStack[0], null),
        ITEMS_KEEPONRESPAWN("items.keepOnRespawn", new ItemStack[0], null),
        ITEMS_KEEPALLONRESPAWN("items.keepAllOnRespawn", false, null),
        ITEMS_MINPLAYERS("items.minplayers", 2, null),
        ITEMS_RANDOM("items.random", true, null),
        ITEMS_REWARDS("items.rewards", new ItemStack[0], null),
        ITEMS_TAKEOUTOFGAME("items.takeOutOfGame", new ItemStack[0], null),

        JOIN_RANGE("join.range", 0, null),
        JOIN_FORCE("join.forceregionjoin", false, null),
        JOIN_ONLYIFHASPLAYED("join.onlyifhasplayed", false, null),

        LISTS_BLACKLIST("block.blacklist", new ArrayList<>(), null),
        LISTS_CMDWHITELIST("cmds.whitelist", new ArrayList<>(), null),
        LISTS_MODS("mods", new ArrayList<>(), null),
        LISTS_WHITELIST("block.whitelist", new ArrayList<>(), null),

        MSG_LOUNGE("msg.lounge", "Welcome to the arena lounge! Hit a class sign and then the iron block to flag yourself as ready!", null),
        MSG_PLAYERJOINED("msg.playerjoined", "%1% joined the Arena!", null),
        MSG_PLAYERJOINEDTEAM("msg.playerjoinedteam", "%1% joined team %2%!", null),
        MSG_STARTING("msg.starting", "Arena is starting! Type &e/pa %1% to join!", null),
        MSG_YOUJOINED("msg.youjoined", "You have joined the FreeForAll Arena!", null),
        MSG_YOUJOINEDTEAM("msg.youjoinedteam", "You have joined team %1%!", null),

        PERMS_ALWAYSJOININBATTLE("perms.alwaysJoinInBattle", false, null),
        PERMS_EXPLICITARENA("perms.explicitArenaNeeded", false, null),
        PERMS_EXPLICITCLASS("perms.explicitClassNeeded", false, null),
        PERMS_FLY("perms.fly", false, null),
        PERMS_LOUNGEINTERACT("perms.loungeinteract", false, null),
        PERMS_JOININBATTLE("perms.joinInBattle", false, null),
        PERMS_JOINWITHSCOREBOARD("perms.joinWithScoreboard", true, null),
        PERMS_TEAMKILL("perms.teamkill", true, null),
        PERMS_SPECTALK("perms.specTalk", true, null),
        PERMS_SPECINTERACT("perms.spectatorinteract", false, null),

        PLAYER_AUTOIGNITE("player.autoIgniteTNT", false, null),
        PLAYER_CLEARINVENTORY("player.clearInventory", "NONE", null),
        PLAYER_COLLISION("player.collision", true, null),
        PLAYER_DROPSEXP("player.dropsEXP", false, null),
        PLAYER_DROPSINVENTORY("player.dropsInventory", false, null),
        PLAYER_EXHAUSTION("player.exhaustion", 0.0, null),
        PLAYER_FEEDFORKILL("player.hungerforkill", 0, null),
        PLAYER_FOODLEVEL("player.foodLevel", 20, null),
        PLAYER_HEALTH("player.health", -1, null),
        PLAYER_HEALFORKILL("player.healforkill", false, null),
        PLAYER_HUNGER("player.hunger", true, null),
        PLAYER_ITEMSONKILL("player.itemsonkill", new ItemStack[0], null),
        PLAYER_MAYCHANGEARMOR("player.mayChangeArmor", true, null),
        PLAYER_MAXHEALTH("player.maxhealth", -1, null),
        PLAYER_REFILLCUSTOMINVENTORY("player.refillCustomInventory", true, null),
        PLAYER_REFILLINVENTORY("player.refillInventory", true, null),
        PLAYER_REFILLFORKILL("player.refillforkill", false, null),
        PLAYER_REMOVEARROWS("player.removearrows", false, null),
        PLAYER_SATURATION("player.saturation", 20, null),
        PLAYER_QUICKLOOT("player.quickloot", false, null),
        PLAYER_TIME("player.dayTime", -1, null),

        PROTECT_ENABLED("protection.enabled", true, null),
        PROTECT_PUNISH("protection.punish", false, null),
        PROTECT_SPAWN("protection.spawn", 0, null),

        READY_AUTOCLASS("ready.autoClass", "none", null),
        READY_BLOCK("ready.block", Material.IRON_BLOCK, null),
        READY_CHECKEACHPLAYER("ready.checkEachPlayer", false, null),
        READY_CHECKEACHTEAM("ready.checkEachTeam", true, null),
        READY_ENFORCECOUNTDOWN("ready.enforceCountdown", false, null),
        READY_MINPLAYERS("ready.minPlayers", 2, null),
        READY_MAXPLAYERS("ready.maxPlayers", 0, null),
        READY_MAXTEAMPLAYERS("ready.maxTeam", 0, null),
        READY_NEEDEDRATIO("ready.neededRatio", 0.5, null),

        TIME_STARTCOUNTDOWN("time.startCountDown", 10, null),
        TIME_ENDCOUNTDOWN("time.endCountDown", 5, null),
        TIME_REGIONTIMER("time.regionTimer", 10, null),
        TIME_TELEPORTPROTECT("time.teleportProtect", 3, null),
        TIME_RESETDELAY("time.resetDelay", -1, null),
        TIME_WARMUPCOUNTDOWN("time.warmupCountDown", 0, null),
        TIME_PVP("time.pvp", 0, null),

        TP_DEATH("tp.death", "old", null),
        TP_EXIT("tp.exit", "old", null),
        TP_LOSE("tp.lose", "old", null),
        TP_WIN("tp.win", "old", null),
        TP_OFFSETS("tp.offsets", new HashMap<>(), null),

        USES_CLASSSIGNSDISPLAY("uses.classSignsDisplay", false, null),
        USES_DEATHMESSAGES("uses.deathMessages", true, null),
        USES_DEATHMESSAGESCUSTOM("uses.deathMessagesCustom", true, null),
        USES_EVENTEAMS("uses.evenTeams", false, null),
        USES_INGAMECLASSSWITCH("uses.ingameClassSwitch", false, null),
        USES_INVISIBILITYFIX("uses.invisibilityfix", false, null),
        USES_EVILINVISIBILITYFIX("uses.evilinvisibilityfix", false, null),
        USES_OVERLAPCHECK("uses.overlapCheck", true, null),
        USES_PLAYERCLASSES("uses.playerclasses", false, null),
        USES_SCOREBOARD("uses.scoreboard", false, null),
        USES_SUICIDEPUNISH("uses.suicidepunish", false, null),
        USES_TEAMREWARDS("uses.teamrewards", false, null),
        USES_TELEPORTONKILL("uses.teleportonkill", false, null),
        USES_WOOLHEAD("uses.woolHead", false, null),

        // ----------
        GOAL_BLOCKDESTROY_BLOCKTYPE("goal.blockdestroy.blocktype", Material.IRON_BLOCK, "BlockDestroy"),
        GOAL_BLOCKDESTROY_LIVES("goal.blockdestroy.bdlives", 1, "BlockDestroy"),

        GOAL_CHECKPOINTS_CLAIMRANGE("goal.checkpoints.cpclaimrange", 5, "CheckPoints"),
        GOAL_CHECKPOINTS_LIVES("goal.checkpoints.cplives", 10, "CheckPoints"),
        GOAL_CHECKPOINTS_TICKINTERVAL("goal.checkpoints.cptickinterval", 20, "CheckPoints"),

        GOAL_DOM_ANNOUNCEOFFSET("goal.dom.spamoffset", 3, "Domination"),
        GOAL_DOM_BOSSBAR("goal.dom.domBossBar", true, "Domination"),
        GOAL_DOM_CLAIMRANGE("goal.dom.claimrange", 3, "Domination"),
        GOAL_DOM_LIVES("goal.dom.dlives", 10, "Domination"),
        GOAL_DOM_ONLYWHENMORE("goal.dom.onlywhenmore", false, "Domination"),
        GOAL_DOM_PARTICLECIRCLE("goal.dom.particlecircle", true, "Domination"),
        GOAL_DOM_TICKINTERVAL("goal.dom.tickinterval", 60, "Domination"),
        GOAL_DOM_TICKREWARD("goal.dom.tickreward", 1, "Domination"),

        GOAL_FLAGS_FLAGTYPE("goal.flags.flagType", Material.WHITE_WOOL, "Flags"),
        GOAL_FLAGS_LIVES("goal.flags.flives", 3, "Flags"),
        GOAL_FLAGS_MUSTBESAFE("goal.flags.mustBeSafe", true, "Flags"),
        GOAL_FLAGS_WOOLFLAGHEAD("goal.flags.woolFlagHead", true, "Flags"),
        GOAL_FLAGS_FLAGEFFECT("goal.flags.effect", "none", "Flags"),
        GOAL_FLAGS_ALTERONCATCH("goal.flags.alterOnCatch", true, "Flags"),

        GOAL_FOOD_FMAXITEMS("goal.food.fmaxitems", 50, "Food"),
        GOAL_FOOD_FPLAYERITEMS("goal.food.fplayeritems", 10, "Food"),
        GOAL_FOOD_FTEAMITEMS("goal.food.fteamitems", 100, "Food"),

        GOAL_INFECTED_ILIVES("goal.infected.iilives", 1, "Infect"),
        GOAL_INFECTED_NLIVES("goal.infected.inlives", 1, "Infect"),
        GOAL_INFECTED_PPROTECTS("goal.infected.iplayerprotect", 0, "Infect"),

        GOAL_LIBERATION_JAILEDSCOREBOARD("goal.liberation.jailedscoreboard", false, "Liberation"),

        GOAL_LLIVES_LIVES("goal.liberation.llives", 3, "Liberation"),
        GOAL_PDM_LIVES("goal.playerdm.pdlives", 3, "PlayerDeathMatch"),

        GOAL_PFLAGS_FLAGTYPE("goal.physicalflags.flagType", Material.WHITE_WOOL, "PhysicalFlags"),
        GOAL_PFLAGS_LIVES("goal.physicalflags.flives", 3, "PhysicalFlags"),
        GOAL_PFLAGS_MUSTBESAFE("goal.physicalflags.mustBeSafe", true, "PhysicalFlags"),
        GOAL_PFLAGS_WOOLFLAGHEAD("goal.physicalflags.woolFlagHead", true, "PhysicalFlags"),
        GOAL_PFLAGS_FLAGEFFECT("goal.physicalflags.effect", "none", "PhysicalFlags"),

        GOAL_PLIVES_LIVES("goal.playerlives.plives", 3, "PlayerLives"),
        GOAL_TANK_LIVES("goal.tank.tlives", 1, "Tank"),
        GOAL_TDC_LIVES("goal.teamdc.tdclives", 10, "TeamDeathConfirm"),
        GOAL_TDC_ITEM("goal.teamdc.tdcitem", Material.WHITE_WOOL, "TeamDeathConfirm"),
        GOAL_TDM_LIVES("goal.teamdm.tdlives", 10, "TeamDeathMatch"),
        GOAL_TDM_SUICIDESCORE("goal.teamdm.suicideScore", false, "TeamDeathMatch"),
        GOAL_TLIVES_LIVES("goal.teamlives.tlives", 10, "TeamLives"),

        GOAL_PILLARS_ANNOUNCETICK("goal.pillars.announcetick", true, "Pillars"),
        GOAL_PILLARS_LIVES("goal.pillars.pillives", 10, "Pillars"),
        GOAL_PILLARS_ONLYFREE("goal.pillars.onlyfree", true, "Pillars"),
        GOAL_PILLARS_BREAKABLE("goal.pillars.breakable", true, "Pillars"),
        GOAL_PILLARS_TICKPOINTS("goal.pillars.tickpoints", 1, "Pillars"),
        GOAL_PILLARS_INTERVAL("goal.pillars.tickinterval", 20, "Pillars"),

        GOAL_PILLARS_ANNOUNCEOFFSET("goal.pillars.announceoffset", 3, "Pillars"),
        GOAL_PILLARS_MAXCLICKS("goal.pillars.maxclicks", 10, "Pillars"),
        GOAL_PILLARS_MAXHEIGHT("goal.pillars.maxheight", 5, "Pillars"),
        GOAL_PILLARS_EMPTYHEIGHT("goal.pillars.emptyheight", 1, "Pillars"),
        GOAL_PILLARS_TEAMHEIGHT("goal.pillars.teamheight", 2, "Pillars"),
        GOAL_PILLARS_CLAIMALL("goal.pillars.claimall", false, "Pillars"),

        GOAL_PLAYERKILLREWARD_GRADUALLYDOWN("goal.playerkillreward.graduallyDown", false, "PlayerKillReward"),
        GOAL_PLAYERKILLREWARD_ONLYGIVE("goal.playerkillreward.onlyGive", false, "PlayerKillReward"),

        // -----------

        MODULES_AFTERMATCH_AFTERMATCH("modules.aftermatch.aftermatch", "off", "AfterMatch"),

        MODULES_ANNOUNCEMENTS_RADIUS("modules.announcements.radius", 0, "Announcements"),
        MODULES_ANNOUNCEMENTS_COLOR("modules.announcements.color", "AQUA", "Announcements"),
        MODULES_ANNOUNCEMENTS_JOIN("modules.announcements.join", false, "Announcements"),
        MODULES_ANNOUNCEMENTS_START("modules.announcements.start", false, "Announcements"),
        MODULES_ANNOUNCEMENTS_END("modules.announcements.end", false, "Announcements"),
        MODULES_ANNOUNCEMENTS_WINNER("modules.announcements.winner", false, "Announcements"),
        MODULES_ANNOUNCEMENTS_LOSER("modules.announcements.loser", false, "Announcements"),
        MODULES_ANNOUNCEMENTS_PRIZE("modules.announcements.prize", false, "Announcements"),
        MODULES_ANNOUNCEMENTS_CUSTOM("modules.announcements.custom", false, "Announcements"),
        MODULES_ANNOUNCEMENTS_ADVERT("modules.announcements.advert", false, "Announcements"),

        MODULES_ARENAMAPS_ALIGNTOPLAYER("modules.arenamaps.aligntoplayer", false, "ArenaMaps"),
        MODULES_ARENAMAPS_SHOWSPAWNS("modules.arenamaps.showspawns", true, "ArenaMaps"),
        MODULES_ARENAMAPS_SHOWPLAYERS("modules.arenamaps.showplayers", true, "ArenaMaps"),
        MODULES_ARENAMAPS_SHOWLIVES("modules.arenamaps.showlives", true, "ArenaMaps"),

        MODULES_ARENAVOTE_EVERYONE("modules.arenavote.everyone", true, "AutoVote"),
        MODULES_ARENAVOTE_AUTOSTART("modules.arenavote.autostart", false, "AutoVote"),
        MODULES_ARENAVOTE_READYUP("modules.arenavote.readyup", 30, "AutoVote"),
        MODULES_ARENAVOTE_ONLYSPAMTOJOIN("modules.arenavote.onlySpamToJOIN", false, "AutoVote"),
        MODULES_ARENAVOTE_SECONDS("modules.arenavote.seconds", 30, "AutoVote"),
        MODULES_ARENAVOTE_WORLD("modules.arenavote.world", "none", "AutoVote"),

        MODULES_BATTLEFIELDGUARD_ENTERDEATH("modules.battlefieldguard.enterdeath", false, "BattleFieldGuard"),

        MODULES_BETTERFIGHT_MESSAGES("modules.betterfight.usemessages", false, "BetterFight"),
        MODULES_BETTERFIGHT_ONEHITITEMS("modules.betterfight.onehititems", "none", "BetterFight"),
        MODULES_BETTERFIGHT_RESETKILLSTREAKONDEATH("modules.betterfight.resetkillstreakondeath", true, "BetterFight"),
        MODULES_BETTERFIGHT_EXPLODEONDEATH("modules.betterfight.explodeondeath", true, "BetterFight"),
        MODULES_BETTERFIGHT_EXPLODEONDEATHONLYONONEHIT("modules.betterfight.explodeondeathonlyononehit", false, "BetterFight"),

        MODULES_BETTERGEARS_HEAD("modules.bettergears.head", true, "BetterGears"),
        MODULES_BETTERGEARS_CHEST("modules.bettergears.chest", true, "BetterGears"),
        MODULES_BETTERGEARS_LEG("modules.bettergears.leg", true, "BetterGears"),
        MODULES_BETTERGEARS_FOOT("modules.bettergears.foot", true, "BetterGears"),
        MODULES_BETTERGEARS_ONLYIFLEATHER("modules.bettergears.onlyifleather", false, "BetterGears"),

        MODULES_BLOCKRESTORE_HARD("modules.blockrestore.hard", false, "BlockRestore"),
        MODULES_BLOCKRESTORE_OFFSET("modules.blockrestore.offset", 1, "BlockRestore"),
        MODULES_BLOCKRESTORE_RESTOREBLOCKS("modules.blockrestore.restoreblocks", true, "BlockRestore"),
        MODULES_BLOCKRESTORE_RESTORECHESTS("modules.blockrestore.restorechests", false, "BlockRestore"),

        MODULES_BLOCKDISSOLVE_CALCOFFSET("modules.blockdissolve.calcoffset", 0.333, "BlockDissolve"),
        MODULES_BLOCKDISSOLVE_MATERIALS("modules.blockdissolve.materials", new ItemStack[]{
                new ItemStack(Material.SNOW_BLOCK, 1),

                new ItemStack(Material.BLACK_WOOL, 1),
                new ItemStack(Material.BLUE_WOOL, 1),
                new ItemStack(Material.CYAN_WOOL, 1),
                new ItemStack(Material.BROWN_WOOL, 1),

                new ItemStack(Material.GRAY_WOOL, 1),
                new ItemStack(Material.GREEN_WOOL, 1),
                new ItemStack(Material.LIGHT_BLUE_WOOL, 1),
                new ItemStack(Material.LIGHT_GRAY_WOOL, 1),

                new ItemStack(Material.LIME_WOOL, 1),
                new ItemStack(Material.MAGENTA_WOOL, 1),
                new ItemStack(Material.ORANGE_WOOL, 1),
                new ItemStack(Material.RED_WOOL, 1),

                new ItemStack(Material.PINK_WOOL, 1),
                new ItemStack(Material.PURPLE_WOOL, 1),
                new ItemStack(Material.YELLOW_WOOL, 1),
                new ItemStack(Material.WHITE_WOOL, 1)}, "BlockDissolve"),
        MODULES_BLOCKDISSOLVE_STARTSECONDS("modules.blockdissolve.startseconds", 10, "BlockDissolve"),
        MODULES_BLOCKDISSOLVE_TICKS("modules.blockdissolve.ticks", 40, "BlockDissolve"),

        MODULES_CHESTFILLER_CHESTLOCATION("modules.chestfiller.chestlocation", "none", "ChestFiller"),
        MODULES_CHESTFILLER_CLEAR("modules.chestfiller.clear", false, "ChestFiller"),
        MODULES_CHESTFILLER_ITEMS("modules.chestfiller.cfitems", new ItemStack[]{new ItemStack(Material.STONE)}, "ChestFiller"),
        MODULES_CHESTFILLER_MAXITEMS("modules.chestfiller.cfmaxitems", 5, "ChestFiller"),
        MODULES_CHESTFILLER_MINITEMS("modules.chestfiller.cfminitems", 0, "ChestFiller"),

        MODULES_COLORTEAMS_HIDENAME("modules.colorteams.hidename", false, "ColorTeams"),
        MODULES_COLORTEAMS_SCOREBOARD("modules.colorteams.scoreboard", false, "ColorTeams"),

        MODULES_DUEL_FORCESTART("modules.duel.forcestart", true, "Duel"),

        MODULES_FIXINVENTORYLOSS_GAMEMODE("modules.fixinventoryloss.gamemode", false, "FixInventoryLoss"),
        MODULES_FIXINVENTORYLOSS_INVENTORY("modules.fixinventoryloss.inventory", false, "FixInventoryLoss"),

        MODULES_ITEMS_INTERVAL("modules.items.interval", 0, "Items"),
        MODULES_ITEMS_ITEMS("modules.items.items", new ItemStack[0], "Items"),

        MODULES_RESPAWNRELAY_INTERVAL("modules.respawnrelay.respawnseconds", 10, "RespawnRelay"),
        MODULES_RESPAWNRELAY_CHOOSESPAWN("modules.respawnrelay.choosespawn", false, "RespawnRelay"),

        MODULES_PLAYERFINDER_MAXRADIUS("modules.playerfinder.maxradius", 100, "PlayerFinder"),

        MODULES_POINTS_GLOBAL("modules.points.global", true, "Points"),

        MODULES_POINTS_REWARD_DEATH("modules.points.reward.PplayerDeath", 0.0d, "Points"),
        MODULES_POINTS_REWARD_KILL("modules.points.reward.PplayerKill", 0.0d, "Points"),
        MODULES_POINTS_REWARD_SCORE("modules.points.reward.PplayerScore", 0.0d, "Points"),
        MODULES_POINTS_REWARD_TRIGGER("modules.points.reward.Ptrigger", 0.0d, "Points"),
        MODULES_POINTS_REWARD_WIN("modules.points.reward.PplayerWin", 0.0d, "Points"),

        MODULES_POWERUPS_DROPSPAWN("modules.powerups.dropspawn", false, "Powerups"),
        MODULES_POWERUPS_USAGE("modules.powerups.usage", "off", "Powerups"),

        MODULES_PROJECTILES_SNOWBALL("modules.projectiles.snowball", true, "Projectiles"),
        MODULES_PROJECTILES_EGG("modules.projectiles.egg", true, "Projectiles"),
        MODULES_PROJECTILES_FISHHOOK("modules.projectiles.fishHook", false, "Projectiles"),
        MODULES_PROJECTILES_ENDERPEARL("modules.projectiles.enderPearl", false, "Projectiles"),

        MODULES_SKINS_VANILLA("modules.skins.vanilla", false, "Skins"),

        MODULES_SPECIALJOIN_SHOWPLAYERS("modules.specialjoin.showplayers", true, "SpecialJoin"),

        MODULES_SQUADS_AUTOSQUAD("modules.squads.autoSquad", "none", "Squads"),
        MODULES_SQUADS_INGAMESWITCH("modules.squads.ingameSquadSwitch", true, "Squads"),

        MODULES_STARTFREEZE_TIMER("modules.startfreeze.freezetimer", 0, "StartFreeze"),

        MODULES_TITLES_COLOR("modules.titles.color", "AQUA", "Titles"),
        MODULES_TITLES_JOIN("modules.titles.join", false, "Titles"),
        MODULES_TITLES_START("modules.titles.start", true, "Titles"),
        MODULES_TITLES_END("modules.titles.end", false, "Titles"),
        MODULES_TITLES_WINNER("modules.titles.winner", true, "Titles"),
        MODULES_TITLES_LOSER("modules.titles.loser", true, "Titles"),
        MODULES_TITLES_PRIZE("modules.titles.prize", false, "Titles"),
        MODULES_TITLES_CUSTOM("modules.titles.custom", false, "Titles"),
        MODULES_TITLES_ADVERT("modules.titles.advert", false, "Titles"),
        MODULES_TITLES_COUNT("modules.titles.count", true, "Titles"),

        MODULES_TURRETS_MAXDEGREES("modules.turrets.maxdegrees", 90.0, "Turrets"),
        MODULES_TURRETS_MININTERVAL("modules.turrets.mininterval", 0, "Turrets"),

        MODULES_VAULT_BETPOT("modules.vault.betpot", false, "Vault"),
        MODULES_VAULT_BETTIME("modules.vault.bettime", 60, "Vault"),
        MODULES_VAULT_BETWINFACTOR("modules.vault.betWinFactor", 1.0d, "Vault"),
        MODULES_VAULT_BETWINTEAMFACTOR("modules.vault.betWinTeamFactor", 1.0d, "Vault"),
        MODULES_VAULT_BETWINPLAYERFACTOR("modules.vault.betWinPlayerFactor", 1.0d, "Vault"),
        MODULES_VAULT_ENTRYFEE("modules.vault.entryfee", 0, "Vault"),
        MODULES_VAULT_KILLREWARD("modules.vault.killreward", 0.0d, "Vault"),
        MODULES_VAULT_MINPLAYTIME("modules.vault.minplaytime", 0, "Vault"),
        MODULES_VAULT_MINPLAYERS("modules.vault.vminplayers", 2, "Vault"),
        MODULES_VAULT_MINIMUMBET("modules.vault.minbet", 0.0d, "Vault"),
        MODULES_VAULT_MAXIMUMBET("modules.vault.maxbet", 0.0d, "Vault"),
        MODULES_VAULT_WINPOT("modules.vault.winPot", false, ""),
        MODULES_VAULT_WINFACTOR("modules.vault.winFactor", 2.0d, "Vault"),
        MODULES_VAULT_WINREWARD("modules.vault.winreward", 0, "Vault"),
        MODULES_VAULT_WINREWARDPLAYERFACTOR("modules.vault.winrewardPlayerFactor", 1.0d, "Vault"),

        MODULES_VAULT_REWARD_DEATH("modules.vault.reward.playerDeath", 0.0d, "Vault"),
        MODULES_VAULT_REWARD_KILL("modules.vault.reward.playerKill", 0.0d, "Vault"),
        MODULES_VAULT_REWARD_SCORE("modules.vault.reward.playerScore", 0.0d, "Vault"),
        MODULES_VAULT_REWARD_TRIGGER("modules.vault.reward.trigger", 0.0d, "Vault"),
        MODULES_VAULT_REWARD_WIN("modules.vault.reward.playerWin", 0.0d, "Vault"),

        MODULES_WALLS_MATERIAL("modules.walls.wallmaterial", Material.SAND, "Walls"),
        MODULES_WALLS_SCOREBOARDCOUNTDOWN("modules.walls.scoreboardcountdown", false, "Walls"),
        MODULES_WALLS_SECONDS("modules.walls.wallseconds", 300, "Walls"),

        MODULES_WORLDEDIT_AUTOLOAD("modules.worldedit.autoload", false, "WorldEdit"),
        MODULES_WORLDEDIT_AUTOSAVE("modules.worldedit.autosave", false, "WorldEdit"),
        MODULES_WORLDEDIT_SCHEMATICPATH("modules.worldedit.schematicpath", "", "WorldEdit"),
        MODULES_WORLDEDIT_REPLACEAIR("modules.worldedit.replaceair", true, "WorldEdit"),
        MODULES_WORLDEDIT_REGIONS("modules.worldedit.regions", new ArrayList<>(), "WorldEdit");

        private final String node;
        private final Object value;
        private final String type;
        private final String goalOrModule;

        public static CFG getByNode(final String node) {
            for (final CFG m : CFG.getValues()) {
                if (m.node.equals(node)) {
                    return m;
                }
            }
            return null;
        }

        CFG(final String node, final String value, final String source) {
            this.node = node;
            this.value = value;
            this.type = "string";
            this.goalOrModule = source;
        }

        CFG(final String node, final Boolean value, final String source) {
            this.node = node;
            this.value = value;
            this.type = "boolean";
            this.goalOrModule = source;
        }

        CFG(final String node, final Integer value, final String source) {
            this.node = node;
            this.value = value;
            this.type = "int";
            this.goalOrModule = source;
        }

        CFG(final String node, final Double value, final String source) {
            this.node = node;
            this.value = value;
            this.type = "double";
            this.goalOrModule = source;
        }

        CFG(final String node, final ItemStack[] value, final String source) {
            this.node = node;
            this.value = getSerializableItemStacks(value);
            this.type = "items";
            this.goalOrModule = source;
        }

        CFG(final String node, final Material value, final String source) {
            this.node = node;
            this.value = value.name();
            this.type = "material";
            this.goalOrModule = source;
        }

        CFG(final String node, final GameMode value, final String source) {
            this.node = node;
            this.value = value.name();
            this.type = "gamemode";
            this.goalOrModule = source;
        }

        CFG(final String node, final List<String> value, String source) {
            this.node = node;
            this.value = value;
            this.type = "list";
            this.goalOrModule = source;
        }

        CFG(final String node, final Map<String, String> value, final String source) {
            this.node = node;
            this.value = node;
            this.type = "stringMap";
            this.goalOrModule = source;
        }

        public String getNode() {
            return this.node;
        }

        public static CommandTree<String> getTabTree() {

            final CommandTree<String> result = new CommandTree<>(null);
            for (final CFG cfg : values()) {
                final String[] split = cfg.node.split("\\.");
                final String ending = split[split.length - 1];

                if ("material".equals(cfg.type)) {
                    result.define(new String[]{cfg.node, "{Material}"});
                    result.define(new String[]{cfg.node, "hand"});
                    result.define(new String[]{ending, "{Material}"});
                    result.define(new String[]{ending, "hand"});
                } else if ("items".equals(cfg.type)) {
                    result.define(new String[]{cfg.node, "inventory"});
                    result.define(new String[]{ending, "inventory"});
                } else if ("gamemode".equals(cfg.type)) {
                    result.define(new String[]{cfg.node, "{GameMode}"});
                    result.define(new String[]{ending, "{GameMode}"});
                } else if ("boolean".equals(cfg.type)) {
                    result.define(new String[]{cfg.node, "true"});
                    result.define(new String[]{cfg.node, "false"});
                    result.define(new String[]{ending, "true"});
                    result.define(new String[]{ending, "false"});
                } else {
                    result.define(new String[]{cfg.node});
                    result.define(new String[]{ending});
                }
            }
            return result;
        }

        @Override
        public String toString() {
            return String.valueOf(this.value);
        }

        public Object getValue() {
            return this.value;
        }

        public static CFG[] getValues() {
            return values();
        }

        public String getType() {
            return this.type;
        }

        public String getGoalOrModule() {
            return this.goalOrModule;
        }

        public boolean hasModule() {
            return this.goalOrModule != null;
        }
    }

    /**
     * Create a new Config instance that uses the specified file for loading and
     * saving.
     *
     * @param configFile a YAML file
     */
    public Config(final File configFile) {
        this.cfg = new YamlConfiguration();
        this.configFile = configFile;
        this.booleans = new HashMap<>();
        this.ints = new HashMap<>();
        this.doubles = new HashMap<>();
        this.strings = new HashMap<>();
    }

    public void createDefaults(final String goal, final List<String> modules) {
        this.cfg.options().indent(2);

        for (final CFG config : CFG.getValues()) {
            if (config.hasModule()) {
                String mod = config.getGoalOrModule();
                if (goal.contains(mod) || modules.contains(mod)) {
                    this.cfg.addDefault(config.getNode(), config.getValue());
                }
            } else {
                this.cfg.addDefault(config.getNode(), config.getValue());
            }
        }
        this.save();
    }

    /**
     * Load the config-file into the YamlConfiguration, and then populate the
     * value maps.
     *
     * @return true, if the load succeeded, false otherwise.
     */
    public boolean load() {
        try {
            this.cfg.load(this.configFile);
            this.reloadMaps();
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Iterates through all keys in the config-file, and populates the value
     * maps. Boolean values are stored in the booleans-map, Strings in the
     * strings-map, etc.
     */
    public void reloadMaps() {
        for (final String s : this.cfg.getKeys(true)) {
            final Object object = this.cfg.get(s);

            if (object instanceof Boolean) {
                this.booleans.put(s, (Boolean) object);
            } else if (object instanceof Integer) {
                this.ints.put(s, (Integer) object);
            } else if (object instanceof Double) {
                this.doubles.put(s, (Double) object);
            } else if (object instanceof String) {
                this.strings.put(s, (String) object);
            }
        }
    }

    /**
     * Save the YamlConfiguration to the config-file.
     *
     * @return true, if the save succeeded, false otherwise.
     */
    public boolean save() {
        try {
            this.cfg.save(this.configFile);
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete the config-file.
     *
     * @return true, if the delete succeeded, false otherwise.
     */
    public boolean delete() {
        return this.configFile.delete();
    }

    // /////////////////////////////////////////////////////////////////////////
    // //
    // GETTERS //
    // //
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Get the YamlConfiguration associated with this Config instance. Note that
     * changes made directly to the YamlConfiguration will cause an
     * inconsistency with the value maps unless reloadMaps() is called.
     *
     * @return the YamlConfiguration of this Config instance
     */
    public YamlConfiguration getYamlConfiguration() {
        return this.cfg;
    }

    /**
     * Retrieve a value from the YamlConfiguration.
     *
     * @param string the path of the value
     * @return the value of the path
     */
    public Object getUnsafe(final String string) {
        return this.cfg.get(string);
    }

    /**
     * Retrieve a boolean from the value maps.
     *
     * @param cfg the node of the value
     * @return the boolean value of the path if the path exists, false otherwise
     */
    public boolean getBoolean(final CFG cfg) {
        return this.getBoolean(cfg, (Boolean) cfg.getValue());
    }

    /**
     * Retrieve a boolean from the value maps.
     *
     * @param cfg the node of the value
     * @param def a default value to return if the value was not in the map
     * @return the boolean value of the path if it exists, def otherwise
     */
    private boolean getBoolean(final CFG cfg, final boolean def) {
        final String path = cfg.getNode();
        final Boolean result = this.booleans.get(path);
        return result == null ? def : result;
    }

    /**
     * Retrieve an int from the value maps.
     *
     * @param cfg the node of the value
     * @return the int value of the path if the path exists, 0 otherwise
     */
    public int getInt(final CFG cfg) {
        return this.getInt(cfg, (Integer) cfg.getValue());
    }

    /**
     * Retrieve an int from the value maps.
     *
     * @param cfg the node of the value
     * @param def a default value to return if the value was not in the map
     * @return the int value of the path if it exists, def otherwise
     */
    public int getInt(final CFG cfg, final int def) {
        final String path = cfg.getNode();
        final Integer result = this.ints.get(path);
        return result == null ? def : result;
    }

    /**
     * Retrieve a double from the value maps.
     *
     * @param cfg the node of the value
     * @return the double value of the path if the path exists, 0D otherwise
     */
    public double getDouble(final CFG cfg) {
        return this.getDouble(cfg, (Double) cfg.getValue());
    }

    /**
     * Retrieve a double from the value maps.
     *
     * @param cfg the node of the value
     * @param def a default value to return if the value was not in the map
     * @return the double value of the path if it exists, def otherwise
     */
    public double getDouble(final CFG cfg, final double def) {
        final String path = cfg.getNode();
        final Double result = this.doubles.get(path);
        return result == null ? def : result;
    }

    /**
     * Retrieve a string from the value maps.
     *
     * @param cfg the node of the value
     * @return the string value of the path if the path exists, null otherwise
     */
    public String getString(final CFG cfg) {
        return this.getString(cfg, (String) cfg.getValue());
    }

    /**
     * Retrieve a string from the value maps.
     *
     * @param cfg the node of the value
     * @param def a default value to return if the value was not in the map
     * @return the string value of the path if it exists, def otherwise
     */
    public String getString(final CFG cfg, final String def) {
        String result = this.strings.get(cfg.getNode());
        return result == null ? def : result;
    }

    /**
     * Retrieve a string from the value maps and return null is value is "none".
     *
     * @param cfg the node of the value
     * @return the string value of the path if the path exists, null otherwise or if value equals "none"
     */
    public String getDefinedString(final CFG cfg) {
        String result = this.getString(cfg);
        return "none".equalsIgnoreCase(result) ? null : result;
    }

    public Material getMaterial(final CFG cfg) {
        return this.getMaterial(cfg, Material.valueOf((String) cfg.getValue()));
    }

    public Material getMaterial(final CFG cfg, final Material def) {
        final String result = this.getDefinedString(cfg);
        if (result == null) {
            return def;
        }
        return Material.valueOf(result);
    }

    public GameMode getGameMode(final CFG cfg) {
        final String result = this.getDefinedString(cfg);
        return GameMode.valueOf(result);
    }

    public ItemStack[] getItems(final CFG cfg) {
        final String path = cfg.getNode();
        try {
            String test = this.getDefinedString(cfg);
            if (test == null) {
                return new ItemStack[0];
            }
        } catch (Exception ignored) {
        }
        try {
            return getItemStacksFromConfig(this.cfg.getList(path));
        } catch (NullPointerException e) {
            return new ItemStack[0];
        }
    }

    public Set<String> getKeys(final String path) {
        if (this.cfg.get(path) == null) {
            return null;
        }

        final ConfigurationSection section = this.cfg.getConfigurationSection(path);
        return section.getKeys(false);
    }

    public List<String> getStringList(final CFG cfg) {
        return this.getStringList(cfg.getNode(), null);
    }

    public List<String> getStringList(final String path, final List<String> def) {
        if (this.cfg.get(path) == null) {
            return def == null ? new LinkedList<>() : def;
        }

        return this.cfg.getStringList(path);
    }

    public Map<String, String> getStringMap(CFG cfg) {
        try {
            ConfigurationSection cs = this.cfg.getConfigurationSection(cfg.getNode());
            Set<String> keys = cs.getKeys(false);

            Map<String, String> results = new HashMap<>();
            keys.forEach(key -> ofNullable(cs.getString(key)).ifPresent(value -> results.put(key, value)));
            return results;
        } catch (NullPointerException e) {
            return new HashMap<>();
        }
    }


    // /////////////////////////////////////////////////////////////////////////
    // //
    // MUTATORS //
    // //
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Set the value of the given path in both the value maps and the
     * YamlConfiguration. Note that this will only properly put the value in its
     * relevant value map, if it is of one of the supported types. The method
     * can also be used to remove values from their maps and the
     * YamlConfiguration by passing null for the value.
     *
     * @param path  the path on which to set the value
     * @param value the value to set
     */
    public void setManually(final String path, final Object value) {
        if (value instanceof Boolean) {
            this.booleans.put(path, (Boolean) value);
        } else if (value instanceof Integer) {
            this.ints.put(path, (Integer) value);
        } else if (value instanceof Double) {
            this.doubles.put(path, (Double) value);
        } else if (value instanceof String) {
            this.strings.put(path, (String) value);
        }

        if (value == null) {
            this.booleans.remove(path);
            this.ints.remove(path);
            this.doubles.remove(path);
            this.strings.remove(path);
        }

        this.cfg.set(path, value);
    }

    public void set(final CFG cfg, final Object value) {
        this.setManually(cfg.getNode(), value);
    }

    // /////////////////////////////////////////////////////////////////////////
    // //
    // UTILITY METHODS //
    // //
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Parse an input string of the form "world,x,y,z" to create a Block
     * Location. This method will only accept strings of the specified form.
     *
     * @param coords a string of the form "world,x,y,z"
     * @return a PABlockLocation in the given world with the given coordinates
     */
    public static PABlockLocation parseBlockLocation(final String coords) {
        final String[] parts = coords.split(",");
        if (parts.length != 4) {
            throw new IllegalArgumentException(
                    "Input string must only contain world, x, y, and z: " + coords);
        }

        final Integer x = parseInteger(parts[1]);
        final Integer y = parseInteger(parts[2]);
        final Integer z = parseInteger(parts[3]);

        if (Bukkit.getWorld(parts[0]) == null || x == null || y == null
                || z == null) {
            throw new IllegalArgumentException(
                    "Some of the parsed values are null: " + coords);
        }

        return new PABlockLocation(parts[0], x, y, z);
    }

    /**
     * Parse an input string of the form "world,x,y,z,yaw,pitch" to create a
     * Block Location. This method will only accept strings of the specified
     * form.
     *
     * @param coords a string of the form "world,x,y,z,yaw,pitch"
     * @return a PALocation in the given world with the given coordinates
     */
    public static PALocation parseLocation(final String coords) {
        String[] parts = coords.split(",");

        if (parts.length == 4) {
            parts = (coords + ",0.0,0.0").split(",");
        }

        if (parts.length != 6) {
            throw new IllegalArgumentException(
                    "Input string must contain world, x, y, z, yaw and pitch: "
                            + coords);
        }

        final Integer x = parseInteger(parts[1]);
        final Integer y = parseInteger(parts[2]);
        final Integer z = parseInteger(parts[3]);
        final Float yaw = parseFloat(parts[4]);
        final Float pitch = parseFloat(parts[5]);

        if (Bukkit.getWorld(parts[0]) == null || x == null || y == null
                || z == null || yaw == null || pitch == null) {
            throw new IllegalArgumentException(
                    "Some of the parsed values are null: " + coords);
        }
        return new PALocation(parts[0], x, y, z, pitch, yaw);
    }

    /**
     *
     */
    public static ArenaRegion parseRegion(final Arena arena,
                                          final YamlConfiguration config, final String regionName) {

        final String coords = config.getString("arenaregion." + regionName);
        final String[] parts = coords.split(",");

        final ArenaRegionShape shape = PVPArena.getInstance().getArsm().getNewInstance(parts[7]);

        if (parts.length < 11) {
            PVPArena.getInstance().getLogger().severe(arena.getName() + " caused an error while loading region " + regionName);
            throw new IllegalArgumentException(
                    "Input string must contain only world, x1, y1, z1, x2, y2, z2, shape and FLAGS: "
                            + coords);
        }
        if (shape == null) {
            PVPArena.getInstance().getLogger().severe(arena.getName() + " caused an error while loading region " + regionName);
            throw new IllegalArgumentException(
                    "Input string does not contain valid region shape: "
                            + coords);
        }
        final Integer x1 = parseInteger(parts[1]);
        final Integer y1 = parseInteger(parts[2]);
        final Integer z1 = parseInteger(parts[3]);
        final Integer x2 = parseInteger(parts[4]);
        final Integer y2 = parseInteger(parts[5]);
        final Integer z2 = parseInteger(parts[6]);
        final Integer flags = parseInteger(parts[8]);
        final Integer prots = parseInteger(parts[9]);

        if (Bukkit.getWorld(parts[0]) == null || x1 == null || y1 == null
                || z1 == null || x2 == null || y2 == null || z2 == null
                || flags == null || prots == null) {
            PVPArena.getInstance().getLogger().severe(arena.getName() + " caused an error while loading region " + regionName);
            throw new IllegalArgumentException(
                    "Some of the parsed values are null!");
        }

        final PABlockLocation[] l = {new PABlockLocation(parts[0], x1, y1, z1),
                new PABlockLocation(parts[0], x2, y2, z2)};

        final ArenaRegion region = new ArenaRegion(arena, regionName, shape, l);
        region.applyFlags(flags);
        region.applyProtections(prots);
        region.setType(RegionType.valueOf(parts[10]));
        region.saveToConfig();

        // "world,x1,y1,z1,x2,y2,z2,shape,FLAGS,PROTS,TYPE"

        return region;
    }

    public static Integer parseInteger(final String string) {
        try {
            return Integer.parseInt(string.trim());
        } catch (final Exception e) {
            return null;
        }
    }

    public static Float parseFloat(final String string) {
        try {
            return Float.parseFloat(string.trim());
        } catch (final Exception e) {
            return null;
        }
    }

    public static String parseToString(final PALocation loc) {
        final String[] result = new String[6];
        result[0] = String.valueOf(loc.getWorldName());
        result[1] = String.valueOf(loc.getBlockX());
        result[2] = String.valueOf(loc.getBlockY());
        result[3] = String.valueOf(loc.getBlockZ());
        result[4] = String.valueOf(loc.getYaw());
        result[5] = String.valueOf(loc.getPitch());
        // "world,x,y,z,yaw,pitch"
        return StringParser.joinArray(result, ",");
    }

    public static String parseToString(final PABlockLocation loc) {
        final String[] result = new String[4];
        result[0] = String.valueOf(loc.getWorldName());
        result[1] = String.valueOf(loc.getX());
        result[2] = String.valueOf(loc.getY());
        result[3] = String.valueOf(loc.getZ());
        // "world,x,y,z"
        return StringParser.joinArray(result, ",");
    }

    public static String parseToString(final ArenaRegion region,
                                       final Set<RegionFlag> flags, final Set<RegionProtection> protections) {
        final String[] result = new String[11];
        result[0] = region.getWorldName();
        result[1] = String.valueOf(region.locs[0].getX());
        result[2] = String.valueOf(region.locs[0].getY());
        result[3] = String.valueOf(region.locs[0].getZ());
        result[4] = String.valueOf(region.locs[1].getX());
        result[5] = String.valueOf(region.locs[1].getY());
        result[6] = String.valueOf(region.locs[1].getZ());
        result[7] = region.getShape().getName();
        result[10] = region.getType().name();

        int sum = 0;

        for (final RegionFlag f : flags) {
            sum += Math.pow(2, f.ordinal());
        }

        result[8] = String.valueOf(sum);

        sum = 0;

        for (final RegionProtection p : protections) {
            sum += Math.pow(2, p.ordinal());
        }
        result[9] = String.valueOf(sum);

        // "world,x1,y1,z1,x2,y2,z2,shape,FLAGS,PROTS,TYPE"
        return StringParser.joinArray(result, ",");
    }

    public void setOffset(String spawnName, double x, double y, double z) {
        Map<String, String> offsets = this.getStringMap(CFG.TP_OFFSETS);
        offsets.put(spawnName, String.format("%.1f;%.1f;%.1f", x, y, z));

        this.cfg.set(CFG.TP_OFFSETS.getNode(), offsets);
        this.save();
    }

    @NotNull
    public Vector getOffset(String spawnName) {
        Map<String, String> offsets = this.getStringMap(CFG.TP_OFFSETS);
        String value = offsets.get(spawnName);

        if (value != null && value.contains(";")) {
            String[] vals = value.split(";");
            try {
                return new Vector(
                        Double.parseDouble(vals[0]),
                        Double.parseDouble(vals[1]),
                        Double.parseDouble(vals[2])
                );
            } catch (Exception ignored) {

            }
        }
        return new Vector(0, 0, 0);
    }
}
