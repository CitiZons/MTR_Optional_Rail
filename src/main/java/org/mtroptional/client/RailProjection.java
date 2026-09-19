package org.mtroptional.client;

import org.mtr.core.data.RailMath;
import org.mtr.core.tool.Vector;

/** Cached polyline projection: continuous progress without per-frame curve sampling. */
public final class RailProjection {
    private final Vector[] points;
    public record Result(double progress, double distanceSquared) {}

    public RailProjection(RailMath math) {
        int segments = Math.max(8, Math.min(128, (int)Math.ceil(math.getLength() / 1.25)));
        points = new Vector[segments + 1];
        for (int i = 0; i <= segments; i++) points[i] = math.getPosition(math.getLength()*i/segments, false);
    }

    public Result project(double x, double y, double z) {
        double bestDistance = Double.POSITIVE_INFINITY, progress = 0;
        for (int i = 0; i < points.length - 1; i++) {
            Vector a = points[i], b = points[i+1];
            double dx=b.x-a.x, dy=b.y-a.y, dz=b.z-a.z;
            double lengthSquared=dx*dx+dy*dy+dz*dz;
            double t=lengthSquared<1E-12 ? 0 : Math.max(0,Math.min(1,((x-a.x)*dx+(y-a.y)*dy+(z-a.z)*dz)/lengthSquared));
            double ex=x-a.x-t*dx, ey=y-a.y-t*dy, ez=z-a.z-t*dz;
            double distance=ex*ex+ey*ey+ez*ez;
            if (distance<bestDistance) { bestDistance=distance; progress=(i+t)/(points.length-1); }
        }
        return new Result(progress,bestDistance);
    }
}
