package net.slipcor.pvparena.classes;

public class PABlock {
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
}
