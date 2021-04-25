package net.slipcor.pvparena.classes;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.core.Config;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.Optional;

public class PASpawn {

    public static final String SPAWN = "spawn";
    public static final String SPECTATOR = "spectator";
    public static final String OLD = "old";
    public static final String EXIT = "exit";
    public static final String PA_SPAWN_FORMAT = "%s%s%s";

    private final PALocation location;
    private final String name;
    private final String teamName;
    private final String className;
    private Vector offset;

    public PASpawn(final PALocation loc, final String name, final String teamName, final String className) {
        this.location = loc;
        this.name = name;
        this.teamName = teamName;
        this.className = className;
    }

    /**
     * Deserialize PaSpawn from a config node
     *
     * format:
     * (team_)<name>(_class): world,x,y,z,yaw,pitch
     * </p>
     * spectator: event,3408,76,135,175.4559326171875,5.699995517730713
     * red_spawn: event,3459,62,104,-90.00845336914062,-1.650019884109497
     * red_spawn_pyro: event,3459,62,104,-90.00845336914062,-1.650019884109497
     *
     * @param spawnNode config node name
     * @param location location
     *
     * @return PaSpawn
     */
    public static PASpawn deserialize(String spawnNode, String location, Arena arena) {

        String[] spawnArgs = spawnNode.split("_");
        if (spawnArgs.length == 1) {
            return new PASpawn(Config.parseLocation(location), spawnNode, null, null);
        } else {
            final ArenaTeam arenaTeam = arena.getTeam(spawnArgs[0]);
            if (arenaTeam == null) {
                PVPArena.getInstance().getLogger().severe(
                        String.format("[%s] %s is not a valid team for spawn %s",
                                arena.getName(),
                                spawnArgs[0],
                                spawnNode
                        ));
                return null;
            }
            if (spawnArgs.length == 2) {
                return new PASpawn(Config.parseLocation(location), spawnArgs[1], arenaTeam.getName(), null);
            } else if (spawnArgs.length == 3) {
                final ArenaClass arenaClass = arena.getClass(spawnArgs[2]);
                if (arenaClass == null) {
                    PVPArena.getInstance().getLogger().severe(
                            String.format("[%s] %s is not a valid class for spawn %s",
                                    arena.getName(),
                                    spawnArgs[2],
                                    spawnNode
                            ));
                } else {
                    return new PASpawn(Config.parseLocation(location), spawnArgs[1], arenaTeam.getName(), arenaClass.getName());
                }
            }
        }
        return null;
    }

    /**
     * Serialize PaSpawn
     *
     * format:
     * (team_)<name>(_class)
     *
     * @param paSpawn spawn
     * @return String of spawn serialized
     */
    public static String serialize(PASpawn paSpawn){

            return (String.format(PA_SPAWN_FORMAT,
                    Optional.ofNullable(paSpawn.getTeamName()).map(tname -> tname + '_').orElse(""),
                    paSpawn.getName(),
                    Optional.ofNullable(paSpawn.getClassName()).map(cname -> cname + '_').orElse("")
            ));
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof PASpawn) {
            final PASpawn other = (PASpawn) o;
            return this.name.equals(other.name)
                    && this.location.equals(other.location)
                    && Objects.equals(this.teamName, other.teamName)
                    && Objects.equals(this.className, other.className);
        }
        return false;
    }

    public PALocation getPALocation() {
        return this.location;
    }

    public String getName() {
        return this.name;
    }

    public String getTeamName() {
        return this.teamName;
    }

    public String getClassName() {
        return this.className;
    }

    public Vector getOffset() {
        return this.offset;
    }

    public void setOffset(Vector offset) {
        this.offset = offset;
    }

    @Override
    public String toString() {
        return "PASpawn{" +
                "location=" + this.location +
                ", spawnName='" + this.name + '\'' +
                ", teamName='" + this.teamName + '\'' +
                ", className='" + this.className + '\'' +
                ", offset=" + this.offset +
                '}';
    }
}
