package org.mtroptional;

/** Immutable values. Offsets are absolute relative to the native node, never accumulated. */
public record NodeSettings(double x, double y, double z, double rotation, double cant) {
    public static final NodeSettings ZERO = new NodeSettings(0, 0, 0, 0, 0);
    public NodeSettings {
        x = validate(x, 1); y = validate(y, 1); z = validate(z, 1);
        rotation = validate(rotation, 90); cant = validate(cant, 45);
    }
    public static double validate(double value, double limit) {
        if (!Double.isFinite(value) || Math.abs(value) > limit) throw new IllegalArgumentException("Out of range");
        return Math.round(value * 100) / 100.0;
    }
    public NodeSettings row(int row, NodeSettings from) {
        return switch (row) {
            case 0 -> new NodeSettings(from.x, from.y, from.z, rotation, cant);
            case 1 -> new NodeSettings(x, y, z, from.rotation, cant);
            case 2 -> new NodeSettings(x, y, z, rotation, from.cant);
            default -> throw new IllegalArgumentException("Unknown row");
        };
    }
}
