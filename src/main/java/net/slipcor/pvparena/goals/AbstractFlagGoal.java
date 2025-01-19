package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.ColorUtils;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringUtils;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.compatibility.Constants.INFINITE_EFFECT_DURATION;


public abstract class AbstractFlagGoal extends ArenaGoal {

    protected static final String TOUCHDOWN = "touchdown";
    protected static final int ENCHANT_LVL_KEY = 770;
    public static final String FLAG = "flag";
    protected Map<ArenaTeam, String> flagMap = new HashMap<>();
    protected Map<ArenaTeam, BlockData> flagDataMap;
    protected Map<ArenaPlayer, ItemStack> headGearMap = new HashMap<>();
    protected String blockTeamName;
    protected ArenaTeam touchdownTeam;

    protected AbstractFlagGoal(String sName) {
        super(sName);
    }

    protected abstract CFG getFlagEffectCfg();

    protected abstract CFG getFlagLivesCfg();

    protected abstract boolean doesAutoColorBlocks();

    protected abstract boolean hasWoolHead();

    protected Map<ArenaTeam, BlockData> getFlagDataMap() {
        if (this.flagDataMap == null) {
            this.flagDataMap = new HashMap<>();
        }
        return this.flagDataMap;
    }

    @Override
    public boolean allowsJoinInBattle() {
        return this.arena.getConfig().getBoolean(CFG.JOIN_ALLOW_DURING_MATCH);
    }

    @Override
    public boolean checkCommand(final String string) {
        return FLAG.equalsIgnoreCase(string);
    }

    @Override
    public List<String> getGoalCommands() {
        return Stream.of(FLAG).collect(Collectors.toList());
    }

    @Override
    public CommandTree<String> getGoalSubCommands(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        arena.getBlocks().stream()
                .filter(block -> FLAG.equalsIgnoreCase(block.getName()))
                .forEach(block -> result.define(new String[]{"remove", block.getTeamName()}));
        arena.getTeamNames().forEach(teamName -> result.define(new String[]{"set", teamName}));
        result.define(new String[]{"set", TOUCHDOWN});
        result.define(new String[]{"effect", "{PotionEffectType}"});
        return result;
    }

    @Override
    public void initiate(final ArenaPlayer arenaPlayer) {
        final ArenaTeam team = arenaPlayer.getArenaTeam();
        if (!this.getTeamLifeMap().containsKey(team)) {
            this.getTeamLifeMap().put(arenaPlayer.getArenaTeam(), this.arena.getConfig().getInt(this.getFlagLivesCfg()));
        }
    }

    @Override
    public void parseStart() {
        this.getTeamLifeMap().clear();
        this.getFlagDataMap().clear();
        for (ArenaTeam arenaTeam : this.arena.getTeams()) {
            if (!arenaTeam.getTeamMembers().isEmpty()) {
                debug(this.arena, "adding team " + arenaTeam.getName());
                // team is active
                this.getTeamLifeMap().put(arenaTeam, this.arena.getConfig().getInt(this.getFlagLivesCfg(), 3));
                Block flagBlock = this.getTeamFlagLoc(arenaTeam).toLocation().getBlock();
                this.colorFlagBlockIfNeeded(arenaTeam, flagBlock);
                this.getFlagDataMap().put(arenaTeam, flagBlock.getBlockData().clone());
            }
        }
        this.touchdownTeam = new ArenaTeam(TOUCHDOWN, "BLACK");
        ofNullable(this.getTeamFlagLoc(this.touchdownTeam)).ifPresent(paBlockLocation -> {
            Block touchdownFlagBlock = paBlockLocation.toLocation().getBlock();
            this.colorFlagBlockIfNeeded(this.touchdownTeam, touchdownFlagBlock);
            this.getFlagDataMap().put(this.touchdownTeam, touchdownFlagBlock.getBlockData().clone());
        });
    }

    @Override
    public void reset(final boolean force) {
        this.getHeadGearMap().clear();
        this.getTeamLifeMap().clear();
        this.getFlagMap().clear();
    }

    @Override
    public void checkJoin(Player player, String[] args) throws GameplayException {
        super.checkJoin(player, args);

        // Check if all blocks are using the same material (color variants accepted)
        List<Material> blocksMaterial = this.arena.getBlocks().stream()
                .map(paBock -> paBock.getLocation().toLocation().getBlock().getType())
                .distinct()
                .collect(Collectors.toList());

        if (!blocksMaterial.stream().allMatch(b -> ColorUtils.isSubType(blocksMaterial.get(0), b))) {
            throw new GameplayException(MSG.ERROR_NEED_SAME_BLOCK_TYPE);
        }
    }

    @Override
    public boolean checkEnd() {
        final int count = TeamManager.countActiveTeams(this.arena);

        if (count == 1) {
            return true;
        } else if (count == 0) {
            debug(this.arena, "No teams playing!");
        }

        return false;
    }

    @Override
    public Set<PASpawn> checkForMissingSpawns(Set<PASpawn> spawns) {
        return SpawnManager.getMissingTeamSpawn(this.arena, spawns);
    }

    @Override
    public Set<PABlock> checkForMissingBlocks(Set<PABlock> blocks) {
        return SpawnManager.getMissingBlocksTeamCustom(this.arena, blocks, FLAG);
    }

    protected void applyEffects(final Player player) {
        final String value = this.arena.getConfig().getDefinedString(this.getFlagEffectCfg());

        if (value == null) {
            return;
        }

        final String[] split = value.split("x");

        int amp = 1;

        if (split.length > 1) {
            try {
                amp = Integer.parseInt(split[1]);
            } catch (final Exception ignored) {

            }
        }

        PotionEffectType pet = PotionEffectType.getByName(split[0]);

        if (pet == null) {
            PVPArena.getInstance().getLogger().warning(
                    "Invalid Potion Effect Definition: " + value);
            return;
        }

        player.addPotionEffect(new PotionEffect(pet, INFINITE_EFFECT_DURATION, amp));
    }

    @Override
    public boolean checkSetBlock(final Player player, final Block block) {

        if (StringUtils.isBlank(this.blockTeamName) || !PAA_Region.activeSelections.containsKey(player.getName())) {
            return false;
        }

        if (block == null) {
            return false;
        }

        return PermissionManager.hasAdminPerm(player) || PermissionManager.hasBuilderPerm(player, this.arena);
    }

    // Available commands
    // flag {set} {team|touchdown}
    // flag {remove} {team|touchdown}
    // flag {effect}

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if(!"flag".equalsIgnoreCase(args[0])) {
            return;
        }

        if("effect".equalsIgnoreCase(args[1])) {
            this.handleEffectCommand(sender, args);
        } else if ("set".equalsIgnoreCase(args[1])) {
            if(args.length != 3) {
                this.arena.msg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), "3");
                return;
            }
            String teamName = args[2];

            if (TOUCHDOWN.equalsIgnoreCase(teamName) || this.arena.getTeam(teamName) != null) {
                this.blockTeamName = teamName;
                String msg = (TOUCHDOWN.equalsIgnoreCase(teamName)) ? TOUCHDOWN : String.format("%s %s", teamName, FLAG);
                this.arena.msg(sender, MSG.GOAL_FLAGS_TOSET, msg);
                PAA_Region.activeSelections.put(sender.getName(), this.arena);

            } else {
                this.arena.msg(sender, MSG.ERROR_TEAM_NOT_FOUND, teamName);
            }

        } else if ("remove".equalsIgnoreCase(args[1])) {
            if(args.length != 3) {
                this.arena.msg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), "3");
                return;
            }

            Optional<PABlock> toRemove = this.arena.getBlocks().stream()
                        .filter(block -> FLAG.equals(block.getName()) && args[2].equalsIgnoreCase(block.getTeamName()))
                        .findAny();

            if(toRemove.isPresent()) {
                SpawnManager.removeBlock(this.arena, toRemove.get());
                this.arena.msg(sender, MSG.GOAL_FLAGS_REMOVED, toRemove.get().getPrettyName());

            } else {
                this.arena.msg(sender, MSG.GOAL_FLAGS_NOTFOUND, args[2]);
            }

        }
    }

    private void handleEffectCommand(CommandSender sender, String[] args) {
        // /pa [arena] flageffect SLOW 2
        if (args.length < 3) {
            this.arena.msg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), "3");
            return;
        }

        if ("none".equalsIgnoreCase(args[2])) {
            this.arena.getConfig().set(this.getFlagEffectCfg(), args[2]);

            this.arena.getConfig().save();
            this.arena.msg(sender, MSG.CFG_SET_DONE, this.getFlagEffectCfg().getNode(), args[2]);
            return;
        }

        PotionEffectType pet = PotionEffectType.getByName(args[2]);

        if (pet == null) {
            this.arena.msg(sender, Language.parse(
                    MSG.ERROR_POTIONEFFECTTYPE_NOTFOUND, args[2]));
            return;
        }

        int amp = 1;

        if (args.length == 5) {
            try {
                amp = Integer.parseInt(args[3]);
            } catch (final Exception e) {
                this.arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[3]);
                return;
            }
        }
        final String value = args[2] + 'x' + amp;
        this.arena.getConfig().set(this.getFlagEffectCfg(), value);

        this.arena.getConfig().save();
        this.arena.msg(sender, MSG.CFG_SET_DONE, this.getFlagEffectCfg().getNode(), value);
    }

    @Override
    public boolean commitSetBlock(final Player player, final Block block) {

        debug(this.arena, player, "trying to set a flag");

        // command : /pa red flag1
        // location: red1flag:

        if (StringUtils.isBlank(this.blockTeamName)) {
            this.arena.msg(player, MSG.ERROR_ERROR, "Flag you are trying to set has no name.");
        } else {
            if(!this.arena.getBlocks().isEmpty()) {
                boolean isSameMaterial = this.isSameTypeThanFlags(block.getType());
                if(!isSameMaterial) {
                    this.arena.msg(player, MSG.NOTICE_NOTICE, Language.parse(MSG.ERROR_NEED_SAME_BLOCK_TYPE));
                }
            }

            SpawnManager.setBlock(this.arena, new PABlockLocation(block.getLocation()), FLAG,
                    Optional.ofNullable(this.blockTeamName).orElse(null));

            String coloredFlagName = ofNullable(this.arena.getTeam(this.blockTeamName))
                    .map(ArenaTeam::getColoredName)
                    .orElse(this.blockTeamName);
            this.arena.msg(player, MSG.GOAL_FLAGS_SET, coloredFlagName);
        }

        PAA_Region.activeSelections.remove(player.getName());
        this.blockTeamName = null;

        return true;
    }

    @Override
    public void commitStart() {
        // empty to kill the error ;)
    }

    @NotNull
    protected Map<ArenaTeam, String> getFlagMap() {
        return this.flagMap;
    }

    /**
     * reset an arena flag
     *
     * @param arenaTeam  team whose flag needs to be reset
     */
    protected void releaseFlag(final ArenaTeam arenaTeam) {
        PABlockLocation paBlockLocation = this.getTeamFlagLoc(arenaTeam);
        if (paBlockLocation == null) {
            return;
        }

        Block flagBlock = paBlockLocation.toLocation().getBlock();
        try {
            flagBlock.setBlockData(this.getFlagDataMap().get(arenaTeam));
        } catch (Exception e) {
            PVPArena.getInstance().getLogger().warning("Impossible to reset flag data ! You may recreate arena flags.");
        }
    }

    protected Material getFlagOverrideTeamMaterial(final Arena arena, final ArenaTeam team) {
        if (arena.getConfig().getUnsafe("flagColors." + team.getName()) == null) {
            if (this.touchdownTeam.equals(team)) {
                return ColorUtils.getWoolMaterialFromChatColor(ChatColor.BLACK);
            }
            return ColorUtils.getWoolMaterialFromChatColor(team.getColor());
        }
        return ColorUtils.getWoolMaterialFromDyeColor(
                (String) arena.getConfig().getUnsafe("flagColors." + team));
    }

    protected Map<ArenaPlayer, ItemStack> getHeadGearMap() {
        if (this.headGearMap == null) {
            this.headGearMap = new HashMap<>();
        }
        return this.headGearMap;
    }

    /**
     * get the team name of the flag a player holds
     *
     * @param arenaPlayer the player to check
     * @return a team name
     */
    protected ArenaTeam getHeldFlagTeam(final ArenaPlayer arenaPlayer) {
        if (this.getFlagMap().isEmpty()) {
            return null;
        }

        debug(arenaPlayer, "getting held FLAG of player {}", arenaPlayer);
        for (ArenaTeam arenaTeam : this.getFlagMap().keySet()) {
            debug(arenaPlayer, "team {} is in {}s hands", arenaTeam, this.getFlagMap().get(arenaTeam));
            if (arenaPlayer.getName().equals(this.getFlagMap().get(arenaTeam))) {
                return arenaTeam;
            }
        }
        return null;
    }

    @Override
    public boolean hasSpawn(final String spawnName, final String spawnTeamName) {

        boolean hasSpawn = super.hasSpawn(spawnName, spawnTeamName);
        if (hasSpawn) {
            return true;
        }

        for (String teamName : this.arena.getTeamNames()) {
            if (spawnName.equalsIgnoreCase(FLAG) && spawnTeamName.equalsIgnoreCase(teamName)) {
                return true;
            }
        }
        return false;
    }

    protected void removeEffects(final Player player) {
        final String value = this.arena.getConfig().getDefinedString(this.getFlagEffectCfg());

        if (value == null) {
            return;
        }

        final String[] split = value.split("x");
        PotionEffectType pet = PotionEffectType.getByName(split[0]);

        if (pet == null) {
            PVPArena.getInstance().getLogger().warning("Invalid Potion Effect Definition: " + value);
            return;
        }

        player.removePotionEffect(pet);
        player.addPotionEffect(new PotionEffect(pet, 0, 1));
    }

    @Override
    public void setDefaults(final YamlConfiguration config) {
        if (config.get("teams") == null) {
            debug(this.arena, "no teams defined, adding custom red and blue!");
            config.addDefault("teams.red", ChatColor.RED.name());
            config.addDefault("teams.blue", ChatColor.BLUE.name());
        }
        if (this.hasWoolHead() || config.get("flagColors") == null) {
            debug(this.arena, "no flag colors defined, adding red and blue!");
            config.addDefault("flagColors.red", DyeColor.RED.name());
            config.addDefault("flagColors.blue", DyeColor.BLUE.name());
        }
    }

    @Override
    public void commitEnd(boolean force) {
        if (this.arena.realEndRunner == null) {
            debug(this.arena, "[CTF] committing end");

            // We have to handle case of forced stop
            ArenaTeam winTeam = this.getTeamLifeMap().entrySet().stream()
                    .filter(entry -> TeamManager.getActiveTeams(this.arena).contains(entry.getKey()))
                    .max(Comparator.comparingInt(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElse(null);


            if (winTeam != null) {

                this.arena.addWinner(winTeam.getName());
                ArenaModuleManager.announce(this.arena, Language.parse(MSG.TEAM_HAS_WON, winTeam.getColoredName()), "WINNER");
                this.arena.broadcast(Language.parse(MSG.TEAM_HAS_WON, winTeam.getColoredName()));

                this.getTeamLifeMap().clear();
                ArenaModuleManager.commitEnd(this.arena, winTeam, null);
                new EndRunnable(this.arena, this.arena.getConfig().getInt(CFG.TIME_ENDCOUNTDOWN));
            }
        } else {
            debug(this.arena, "[CTF] already ending");
        }
    }

    protected void commit(final Arena arena, final ArenaTeam arenaTeam) {
        debug(arena, "[CTF] committing end: " + arenaTeam);

        for (ArenaTeam team : arena.getTeams()) {
            if (!team.equals(arenaTeam)) {
                continue;
            }
            for (ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                arenaPlayer.setStatus(PlayerStatus.LOST);
            }
        }

        WorkflowManager.handleEnd(this.arena, false);
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (ArenaTeam team : this.arena.getTeams()) {
            double score = this.getTeamLifeMap().getOrDefault(team, 0);
            if (scores.containsKey(team.getName())) {
                scores.put(team.getName(), scores.get(team.getName()) + score);
            } else {
                scores.put(team.getName(), score);
            }
        }

        return scores;
    }

    @Override
    public void unload(final ArenaPlayer arenaPlayer) {
        this.disconnect(arenaPlayer);
        if (this.allowsJoinInBattle()) {
            this.arena.hasNotPlayed(arenaPlayer);
        }
    }

    protected boolean isIrrelevantInventoryClickEvent(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        Arena arena = arenaPlayer.getArena();

        if (arena == null || !arena.getName().equals(this.arena.getName())) {
            return true;
        }

        if (event.isCancelled() || this.getHeldFlagTeam(arenaPlayer) == null) {
            return true;
        }

        if (event.getInventory().getType() == InventoryType.CRAFTING && event.getRawSlot() != 5) {
            return true;
        }

        return event.getCurrentItem() == null || !InventoryType.PLAYER.equals(event.getInventory().getType());
    }

    protected void checkAndCommitTouchdown(final Arena arena, final ArenaTeam playerTeam) {
        debug(arena, "touchdown reducing lives of all teams excepting: " + playerTeam);
        this.arena.getTeams().stream()
                .filter(arenaTeam -> !playerTeam.getName().equals(arenaTeam.getName()))
                .forEach(arenaTeam -> this.reduceLivesCheckEndAndCommit(arena, arenaTeam));
    }

    protected void reduceLivesCheckEndAndCommit(final Arena arena, final ArenaTeam team) {
        debug(arena, "reducing lives of team " + team);
        if (this.getTeamLifeMap().get(team) != null) {
            final int iLives = this.getTeamLifeMap().get(team) - 1;
            if (iLives > 0) {
                this.getTeamLifeMap().put(team, iLives);
            } else {
                this.getTeamLifeMap().remove(team);
                this.commit(arena, team);
            }
        }
    }

    protected PABlockLocation getTeamFlagLoc(ArenaTeam arenaTeam) {
        return SpawnManager.getBlockByExactName(this.arena, FLAG, arenaTeam.getName());
    }

    /**
     * Check if the material is the same than the one used for the arena flags
     * Reminder: arena flags have to be all the same
     *
     * The function adds a special comparison for banners because dropped "wall_banners" return a "banner" material
     * @param materialToCheck the material to compare with flags
     * @return True if it's the same material
     */
    protected boolean isSameTypeThanFlags(Material materialToCheck) {
        return this.getFlagDataMap().values().stream()
                .findAny()
                .map(flagBlockData -> {
                    Material reference = flagBlockData.getMaterial();
                    return (ColorUtils.isSubType(materialToCheck, reference)) || (Tag.BANNERS.isTagged(materialToCheck) && Tag.BANNERS.isTagged(reference));
                })
                .orElse(true);
    }

    private void colorFlagBlockIfNeeded(ArenaTeam team, Block block) {
        if (this.doesAutoColorBlocks() && ColorUtils.isColorableMaterial(block.getType())) {
            ColorUtils.setNewFlagColor(block, team.getColor());
        }
    }
}
