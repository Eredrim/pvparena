package net.slipcor.pvparena.classes;

import java.util.Optional;

public class PABlock {

    public static final String PA_BLOCK_FORMAT = "%s%s";
    public static final String PA_BLOCK_TEAM_FORMAT = "%s_%s";

    private final PABlockLocation location;
    private final String name;
    private final String teamName;

    public PABlock(final PABlockLocation loc, final String name, final String teamName) {
        this.location = loc;
        this.name = name;
        this.teamName = teamName;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof PABlock) {
            final PABlock other = (PABlock) o;
            return this.name.equals(other.name) && this.location.equals(other.location);
        }
        return false;
    }

    public PABlockLocation getLocation() {
        return this.location;
    }

    public String getName() {
        return this.name;
    }

    public String getTeamName() {
        return this.teamName;
    }

    /**
     * Serialize PaBlock
     * <p>
     * format:
     * (team_)<name>
     *
     * @param paBlock block
     * @return String of block serialized
     */
    public static String serialize(PABlock paBlock) {
        return paBlock.getTeamName() != null ?
                String.format(PA_BLOCK_TEAM_FORMAT, paBlock.getTeamName(), paBlock.getName()) :
                paBlock.getName();
    }
}
