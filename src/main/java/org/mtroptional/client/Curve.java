package org.mtroptional.client;

import org.mtr.core.data.RailMath;
import org.mtr.core.tool.Vector;
import org.mtroptional.NodeSettings;

/** A Hermite correction preserves MTR's vertical shape and rotates its endpoint tangents. */
public final class Curve {
    public final RailMath math;
    public final NodeSettings a, b;
    public final double length;
    private final Vector da, db;
    public Curve(RailMath math, NodeSettings a, NodeSettings b) {
        this.math=math; this.a=a; this.b=b; length=math.getLength();
        double step=Math.min(0.001,length/1000);
        Vector start=math.getPosition(0,false), startNext=math.getPosition(step,false);
        Vector endPrev=math.getPosition(length-step,false), end=math.getPosition(length,false);
        da=correction(startNext.add(start.multiply(-1,-1,-1)).multiply(length/step,0,length/step),a.rotation());
        db=correction(end.add(endPrev.multiply(-1,-1,-1)).multiply(length/step,0,length/step),b.rotation());
    }
    private static Vector correction(Vector v,double degrees) {
        double angle=Math.toRadians(degrees), c=Math.cos(angle),s=Math.sin(angle);
        return new Vector(v.x*c-v.z*s-v.x,0,v.x*s+v.z*c-v.z);
    }
    public Vector point(double t) {
        t=Math.max(0,Math.min(1,t));
        return math.getPosition(t*length,false).add(offset(t));
    }
    /** The authored displacement, shared by rails, car bodies and bogies. */
    public Vector offset(double t) {
        t=Math.max(0,Math.min(1,t));
        double t2=t*t,t3=t2*t;
        double h0=2*t3-3*t2+1,h1=-2*t3+3*t2,h2=t3-2*t2+t,h3=t3-t2;
        return new Vector(h0*a.x()+h1*b.x()+h2*da.x+h3*db.x,verticalOffset(a,b,t),h0*a.z()+h1*b.z()+h2*da.z+h3*db.z);
    }
    public Vector tangent(double t) {
        Vector delta=point(t+0.00001).add(point(t-0.00001).multiply(-1,-1,-1));
        double horizontal=Math.hypot(delta.x,delta.z);
        return horizontal<1E-10 ? new Vector(1,0,0) : delta.multiply(1/horizontal,1/horizontal,1/horizontal);
    }
    public static double verticalOffset(NodeSettings a, NodeSettings b, double t) {
        t=Math.max(0,Math.min(1,t));
        double s=t*t*(3-2*t);
        return a.y()*(1-s)+b.y()*s;
    }
    public double cant(double t) { double s=t*t*(3-2*t); return a.cant()*(1-s)+b.cant()*s; }
}
