package org.mtroptional.client;

import org.mtroptional.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.mtr.core.data.Position;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;

public final class ClientNodes {
    private static final Map<String, Map<Long, RailNetwork.State>> DIMENSIONS = new HashMap<>();
    public static void clear() { DIMENSIONS.clear(); RailTiltClient.clear(); }
    public static void receive(RailNetwork.State state) {
        Map<Long, RailNetwork.State> values = DIMENSIONS.computeIfAbsent(state.dimension(), ignored -> new HashMap<>());
        if (state.clear()) { values.clear(); RailTiltClient.clear(); }
        else values.put(state.pos(), state);
        EditorEvents.updated(state);
    }
    public static RailNetwork.State state(long pos) {
        var level = Minecraft.getInstance().level;
        String dimension = level == null ? "" : level.dimension().location().toString();
        return DIMENSIONS.getOrDefault(dimension, Map.of()).getOrDefault(pos, new RailNetwork.State(dimension,false,pos,NodeSettings.ZERO,0,0));
    }
    public static NodeSettings get(long pos) { return state(pos).value(); }
    public static int getNativeSpeed(BlockPos pos) {
        Position endpoint = new Position(pos.getX(), pos.getY(), pos.getZ());
        double yaw = Math.toRadians(Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.getYRot());
        double facingX = -Math.sin(yaw), facingZ = Math.cos(yaw);
        int best = 0;
        var connected = MinecraftClientData.getInstance().positionsToRail.get(endpoint);
        if (connected == null) return 0;
        for (var entry : connected.entrySet()) {
            Position other = entry.getKey();
            double dx = other.getX() - endpoint.getX(), dz = other.getZ() - endpoint.getZ();
            if (dx * facingX + dz * facingZ > 0.0) {
                // MTR exposes the endpoint-aware speed in meters/ms; convert using the same factor as its UI.
                var rail = entry.getValue();
                // Rail stores the native MTR type speeds in meters/ms (300 kph diamond,
                // 200 kph quartz, etc.); use the endpoint direction where available.
                double nativeSpeed = rail.getSpeedLimitMetersPerMillisecond(endpoint);
                if (nativeSpeed <= 0) nativeSpeed = Math.max(rail.speedLimit1MetersPerMillisecond, rail.speedLimit2MetersPerMillisecond);
                best = Math.max(best, (int)Math.round(nativeSpeed * 3600000D));
            }
        }
        // The editor can be opened from either side of a node; if the player's
        // view does not point along the outgoing rail, still expose its native type speed.
        if (best == 0) for (var entry : connected.entrySet()) {
            var rail = entry.getValue();
            double nativeSpeed = rail.getSpeedLimitMetersPerMillisecond(endpoint);
            if (nativeSpeed <= 0) nativeSpeed = Math.max(rail.speedLimit1MetersPerMillisecond, rail.speedLimit2MetersPerMillisecond);
            best = Math.max(best, (int)Math.round(nativeSpeed * 3600000D));
        }
        if (best == 0) for (var rail : MinecraftClientData.getInstance().railIdMap.values()) {
            if (rail == null) continue;
            var ends = new org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet<Position>(); rail.writePositions(ends);
            if (ends.contains(endpoint)) {
                double nativeSpeed = rail.getSpeedLimitMetersPerMillisecond(endpoint);
                if (nativeSpeed <= 0) nativeSpeed = Math.max(rail.speedLimit1MetersPerMillisecond, rail.speedLimit2MetersPerMillisecond);
                best = Math.max(best, (int)Math.round(nativeSpeed * 3600000D));
            }
        }
        return best;
    }

    /** Returns every native MTR directional speed currently loaded, stably sorted and deduplicated. */
    public static List<Double> nativeSpeedCatalog() {
        TreeSet<Double> speeds = new TreeSet<>();
        for (var rail : MinecraftClientData.getInstance().railIdMap.values()) {
            if (rail == null) continue;
            addNativeSpeed(speeds, rail.speedLimit1MetersPerMillisecond);
            addNativeSpeed(speeds, rail.speedLimit2MetersPerMillisecond);
        }
        return List.copyOf(new ArrayList<>(speeds));
    }

    private static void addNativeSpeed(TreeSet<Double> speeds, double metersPerMillisecond) {
        if (metersPerMillisecond > 0 && Double.isFinite(metersPerMillisecond)) {
            speeds.add(Math.round(metersPerMillisecond * 3600000D * 10D) / 10D);
        }
    }
}

