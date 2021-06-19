package net.slipcor.pvparena.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandTree<T> {
    private final T key;
    private final List<CommandTree<T>> children = new ArrayList<>();

    public CommandTree(final T key) {
        this.key = key;
    }

    public List<T> getContent() {
        final List<T> result = new ArrayList<>();
        for (CommandTree<T> tree : this.children) {
            result.add(tree.key);
        }
        return result;
    }

    public void define(final T[] args) {
        if (args.length > 1) {
            final CommandTree<T> tree;
            if (this.contains(args[0])) {
                tree = this.get(args[0]);
            } else {
                tree = new CommandTree<>(args[0]);
                this.children.add(tree);
            }
            tree.define(Arrays.copyOfRange(args, 1, args.length));
        } else {
            // one argument left
            if (this.contains(args[0])) {
                // child exists; better remove
                this.remove(args[0]);
            }
            this.children.add(new CommandTree<>(args[0]));
        }
    }

    public boolean contains(final T key) {
        for (CommandTree<T> tree : this.children) {
            if (tree.key.equals(key)) {
                return true;
            }
            if (tree.contains(key)) {
                return true;
            }
        }
        return false;
    }

    public CommandTree<T> get(final T key) {
        for (CommandTree<T> tree : this.children) {
            if (tree.key.equals(key)) {
                return tree;
            }
            if (tree.contains(key)) {
                return tree.get(key);
            }
        }
        return null;
    }

    private boolean remove(final T key) {
        for (CommandTree<T> tree : this.children) {
            if (tree.key.equals(key)) {
                this.children.remove(tree);
                return true;
            }
            if (tree.contains(key)) {
                return tree.remove(key);
            }
        }
        return false;
    }
/*
    public void debug(final int level) {
        final StringBuffer c = new StringBuffer();
        for (int pos=0; pos<level; pos++) {
            c.append('-');
        }
        System.out.println("");
        System.out.println("-----------");
        System.out.print(c.toString()+"key:");
        System.out.println(key);
        System.out.print(c.toString()+"children:");
        for (CommandTree<T> child : children) {
            child.debug(level+1);
        }
    }*/
}
