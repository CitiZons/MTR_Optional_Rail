package org.mtroptional;

public final class NodeHistory {
    public NodeSettings current = NodeSettings.ZERO;
    public final NodeSettings[] previous = new NodeSettings[3];
    public long revision;
    public boolean edit(int row, boolean undo, NodeSettings next) {
        if (row < 0 || row > 2) return false;
        if (undo) {
            if (previous[row] == null) return false;
            current = current.row(row, previous[row]);
            previous[row] = null;
        } else {
            NodeSettings changed = current.row(row, next);
            if (changed.equals(current)) return false;
            previous[row] = current;
            current = changed;
        }
        revision++;
        return true;
    }
    public int undoMask() {
        int mask = 0;
        for (int i = 0; i < 3; i++) if (previous[i] != null) mask |= 1 << i;
        return mask;
    }
}
