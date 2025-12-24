package net.slipcor.pvparena.api;

import net.slipcor.pvparena.arena.Arena;

import java.util.Arrays;

/**
 * A simple wrapper used to simplify placeholder arguments handling
 *
 * Base on the following principle:
 * - Arena is always the first part of identifier
 * - Action (what user ask to parse) is always the second part of the identifier
 */
public class PlaceholderArgs {
    private final Arena arena;

    private final String identifier;

    private final String[] args;

    public PlaceholderArgs(Arena arena, String identifier) {
        this.arena = arena;
        this.args = identifier.split("_");
        this.args[0] = arena.getName();
        this.identifier = String.join("_", this.args);
    }

    public PlaceholderArgs(String identifier) {
        this.arena = null;
        this.args = String.format("global_%s", identifier).split("_");
        this.identifier = String.join("_", this.args);
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public int getArgsLength() {
        return this.args.length;
    }

    public String getArg(int index) {
        try {
            return this.args[index];
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public String getAction() {
       return this.getArg(1).toLowerCase();
    }

    public Arena getArena() {
        return this.arena;
    }

    public boolean argEquals(int index, String toCompare) {
        try {
            return this.args[index].equalsIgnoreCase(toCompare);
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    public String getIdentifierUntil(int maxIndex) {
        return String.join("_", Arrays.copyOf(this.args, maxIndex + 1));
    }
}
