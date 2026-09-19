package org.mtroptional.client;

import org.mtr.core.data.Position;
import org.mtr.core.data.Rail;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.mtr.mod.client.MinecraftClientData;
import org.mtroptional.NodeSettings;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Client-only nearest-rail query shared by cars, passengers, and the camera. */
public final class RailTiltClient {
    private static final double MAX_DISTANCE_SQ = 25;
    private static final ConcurrentHashMap<Long, Rail> LOCAL_RAIL_CACHE = new ConcurrentHashMap<>();
    private RailTiltClient() {}

    public record RailPose(double cant, Vector offset) {
        private static final RailPose ZERO = new RailPose(0, new Vector(0,0,0));
    }
    public static double getSignedCant(double x, double y, double z, double forwardX, double forwardZ) {
        return getPose(x, y, z, forwardX, forwardZ).cant();
    }
    /** Query both corrections once at the original world position, before riding-relative transforms. */
    public static RailPose getPose(double x, double y, double z, double forwardX, double forwardZ) {
        Rail best = null;
        double bestDistance = MAX_DISTANCE_SQ, bestProgress = 0;
        MinecraftClientData data = MinecraftClientData.getInstance();
        Rail cachedRail = cached(x,y,z);
        // Validate cached rails against the current dimension and include their neighbours at nodes.
        java.util.Set<Rail> candidates = new java.util.HashSet<>();
        if (cachedRail != null && data.railIdMap.get(cachedRail.getHexId()) == cachedRail) {
            candidates.add(cachedRail);
            ObjectArraySet<Position> ends = new ObjectArraySet<>(); cachedRail.writePositions(ends);
            for (Position end : ends) {
                var connected = data.positionsToRail.get(end);
                if (connected != null) candidates.addAll(connected.values());
            }
        }
        for (int pass = 0; pass < 2; pass++) {
            for (Rail rail : pass == 0 ? candidates : data.railIdMap.values()) {
                if (rail == null || rail.railMath.getLength() < .001) continue;
                RailProjection.Result projection = samples(rail).project(x,y,z);
                if (projection.distanceSquared() < bestDistance) {
                    bestDistance = projection.distanceSquared(); best = rail; bestProgress = projection.progress();
                }
            }
            if (best != null) break;
        }
        if (LOCAL_RAIL_CACHE.size() > 2048) LOCAL_RAIL_CACHE.clear();
        if (best != null) LOCAL_RAIL_CACHE.put(cacheKey(x,y,z),best);
        if (best == null) return RailPose.ZERO;
        ObjectArraySet<Position> positions = new ObjectArraySet<>();
        best.writePositions(positions);
        if (positions.size() != 2) return RailPose.ZERO;
        Position[] ends = positions.toArray(new Position[0]);
        Vector railStart = best.railMath.getPosition(0, false);
        if (distance(ends[0], railStart) > distance(ends[1], railStart)) {
            Position tmp = ends[0]; ends[0] = ends[1]; ends[1] = tmp;
        }
        NodeSettings start = RailGeometry.orient(ClientNodes.get(blockKey(ends[0])), ends[0],
            best.railMath.getPosition(Math.min(.01,best.railMath.getLength()),false).add(railStart.multiply(-1,-1,-1)));
        NodeSettings end = RailGeometry.orient(ClientNodes.get(blockKey(ends[1])), ends[1],
            best.railMath.getPosition(best.railMath.getLength(),false).add(best.railMath.getPosition(Math.max(0,best.railMath.getLength()-.01),false).multiply(-1,-1,-1)));
        double cant = start.cant() * (1 - smooth(bestProgress)) + end.cant() * smooth(bestProgress);
        Vector p1 = best.railMath.getPosition(Math.max(0, best.railMath.getLength() * bestProgress - .2), false);
        Vector p2 = best.railMath.getPosition(Math.min(best.railMath.getLength(), best.railMath.getLength() * bestProgress + .2), false);
        double railForwardX = p2.x() - p1.x(), railForwardZ = p2.z() - p1.z();
        return new RailPose(railForwardX * forwardX + railForwardZ * forwardZ < 0 ? -cant : cant,
            new Curve(best.railMath, start, end).offset(bestProgress));
    }
    public static double getUnsignedCant(double x,double y,double z) { return getSignedCant(x,y,z,1,0); }
    public record VehicleTransform(double x, double y, double z, double yawDegrees) {}

    /** Moves rendered cars to the authored visual rail centerline and rotates their tangent. */
    public static VehicleTransform transformVehicle(double x, double y, double z, double yawRadians) {
        Rail best = cached(x,y,z); double bestDistance = MAX_DISTANCE_SQ; double progress = 0;
        if (best == null) for (Rail rail : MinecraftClientData.getInstance().railIdMap.values()) {
            if (rail == null || rail.railMath.getLength() < .001) continue;
            double length = rail.railMath.getLength();
            int samples = Math.max(8, Math.min(48, (int) Math.ceil(length / 2)));
            double localDistance = Double.MAX_VALUE, localProgress = 0;
            for (int i = 0; i <= samples; i++) {
                double t = (double) i / samples;
                Vector p = rail.railMath.getPosition(length * t, false);
                double d = square(p.x() - x) + square(p.y() - y) + square(p.z() - z);
                if (d < localDistance) { localDistance = d; localProgress = t; }
            }
            if (localDistance < bestDistance) { bestDistance = localDistance; best = rail; progress = localProgress; }
        }
        if (best == null) return new VehicleTransform(x, y, z, Math.toDegrees(yawRadians));
        LOCAL_RAIL_CACHE.put(cacheKey(x, y, z), best);
        if (bestDistance == MAX_DISTANCE_SQ) {
            double length = best.railMath.getLength();
            for (int i = 0; i <= 16; i++) {
                double t = i / 16D; Vector p = best.railMath.getPosition(length * t, false);
                double d = square(p.x() - x) + square(p.y() - y) + square(p.z() - z);
                if (d < bestDistance) { bestDistance = d; progress = t; }
            }
        }
        // Refine the coarse cached sample locally. This avoids a full rail scan for every vehicle.
        double length = best.railMath.getLength();
        double lo = Math.max(0, progress - 0.08), hi = Math.min(1, progress + 0.08);
        for (int i = 0; i <= 8; i++) {
            double t = lo + (hi - lo) * i / 8D;
            Vector p = best.railMath.getPosition(length * t, false);
            double d = square(p.x() - x) + square(p.y() - y) + square(p.z() - z);
            if (d < bestDistance) { bestDistance = d; progress = t; }
        }
        ObjectArraySet<Position> positions = new ObjectArraySet<>(); best.writePositions(positions);
        if (positions.size() != 2) return new VehicleTransform(x, y, z, Math.toDegrees(yawRadians));
        Position[] ends = positions.toArray(new Position[0]);
        Vector start = best.railMath.getPosition(0, false);
        if (distance(ends[0], start) > distance(ends[1], start)) { Position tmp = ends[0]; ends[0] = ends[1]; ends[1] = tmp; }
        NodeSettings a = ClientNodes.get(blockKey(ends[0])), b = ClientNodes.get(blockKey(ends[1]));
        double railLength = best.railMath.getLength();
        Vector here = best.railMath.getPosition(railLength * progress, false);
        Vector tangent = best.railMath.getPosition(Math.min(railLength, railLength * progress + .15), false);
        double tx = tangent.x() - here.x(), tz = tangent.z() - here.z();
        double forwardX = Math.sin(yawRadians), forwardZ = Math.cos(yawRadians);
        double directedProgress = tx * forwardX + tz * forwardZ < 0 ? 1 - progress : progress;
        double s = smooth(directedProgress);
        double ox = a.x() * (1 - s) + b.x() * s, oy = a.y() * (1 - s) + b.y() * s, oz = a.z() * (1 - s) + b.z() * s;
        double railYaw = Math.toDegrees(Math.atan2(tx, tz));
        return new VehicleTransform(here.x() + ox, here.y() + oy, here.z() + oz,
            railYaw + a.rotation() * (1 - s) + b.rotation() * s);
    }
    private record SampleEntry(Rail rail, RailProjection projection) {}
    private static final Map<String, SampleEntry> SAMPLES = new java.util.HashMap<>();
    private static RailProjection samples(Rail rail) {
        SampleEntry entry = SAMPLES.get(rail.getHexId());
        if (entry == null || entry.rail() != rail) {
            if (SAMPLES.size() >= 512) SAMPLES.clear();
            entry = new SampleEntry(rail, new RailProjection(rail.railMath));
            SAMPLES.put(rail.getHexId(), entry);
        }
        return entry.projection();
    }
    public static void clear() { LOCAL_RAIL_CACHE.clear(); SAMPLES.clear(); }
    private static double smooth(double x) { return x*x*(3-2*x); }
    private static double square(double x) { return x*x; }
    private static double distance(Position p, Vector v) { return square(p.getX()+.5-v.x()) + square(p.getY()-v.y()) + square(p.getZ()+.5-v.z()); }
    private static long blockKey(Position p) { return net.minecraft.core.BlockPos.asLong((int)p.getX(), (int)p.getY(), (int)p.getZ()); }
    private static long cacheKey(double x,double y,double z) { return net.minecraft.core.BlockPos.asLong((int)Math.floor(x/2),(int)Math.floor(y/2),(int)Math.floor(z/2)); }
    private static Rail cached(double x,double y,double z) { return LOCAL_RAIL_CACHE.get(cacheKey(x,y,z)); }
}
