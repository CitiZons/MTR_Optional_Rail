package org.mtroptional.client;

import net.minecraft.core.BlockPos;
import org.mtr.core.data.*;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.mtroptional.NodeSettings;

public final class RailGeometry {
    public static final ThreadLocal<Frame> FRAME = new ThreadLocal<>();
    public record Frame(Vector a, Vector b, Vector ta, Vector tb, double cantA, double cantB) {
        public Vector bank(double x,double y,double z) {
            // Use the closest endpoint of this render segment, independently of vertex winding.
            double d1=(x-a.x)*(x-a.x)+(z-a.z)*(z-a.z), d2=(x-b.x)*(x-b.x)+(z-b.z)*(z-b.z);
            Vector center=d1<=d2?a:b, tangent=d1<=d2?ta:tb;
            double angle=Math.toRadians(d1<=d2?cantA:cantB);
            double lateral=(x-center.x)*tangent.z-(z-center.z)*tangent.x;
            double height=y-center.y;
            double newLateral=lateral*Math.cos(angle)-height*Math.sin(angle);
            double newHeight=lateral*Math.sin(angle)+height*Math.cos(angle);
            return new Vector(x+(newLateral-lateral)*tangent.z,center.y+newHeight,z-(newLateral-lateral)*tangent.x);
        }
    }
    public static void render(Rail rail,RailMath.RenderRail callback,double interval,float radius1,float radius2) {
        ObjectArraySet<Position> positions=new ObjectArraySet<>(); rail.writePositions(positions);
        if (positions.size()!=2 || rail.railMath.getLength()<0.001) { rail.railMath.render(callback,interval,radius1,radius2); return; }
        Position[] ends=positions.toArray(new Position[0]);
        Vector start=rail.railMath.getPosition(0,false);
        if (distance(ends[0],start)>distance(ends[1],start)) { Position temp=ends[0]; ends[0]=ends[1]; ends[1]=temp; }
        NodeSettings a=ClientNodes.get(key(ends[0])), b=ClientNodes.get(key(ends[1]));
        if (a.equals(NodeSettings.ZERO)&&b.equals(NodeSettings.ZERO)) { rail.railMath.render(callback,interval,radius1,radius2); return; }
        // Bank sign follows the node's block orientation, so both sides of a junction share a plane.
        a=orient(a,ends[0],rail.railMath.getPosition(Math.min(0.01,rail.railMath.getLength()),false).add(start.multiply(-1,-1,-1)));
        Vector end=rail.railMath.getPosition(rail.railMath.getLength(),false);
        b=orient(b,ends[1],end.add(rail.railMath.getPosition(Math.max(0,rail.railMath.getLength()-0.01),false).multiply(-1,-1,-1)));
        Curve curve=new Curve(rail.railMath,a,b);
        int count=Math.max(1,(int)Math.ceil(curve.length/(interval>0?interval:0.5)));
        Frame old=FRAME.get();
        try {
            Vector p=curve.point(0), tangent=curve.tangent(0);
            for (int i=0;i<count;i++) {
                double t0=(double)i/count,t1=(double)(i+1)/count;
                Vector q=curve.point(t1),nextTangent=curve.tangent(t1);
                FRAME.set(new Frame(p,q,tangent,nextTangent,curve.cant(t0),curve.cant(t1)));
                callback.renderRail(p.x+tangent.z*radius1,p.z-tangent.x*radius1,p.x+tangent.z*radius2,p.z-tangent.x*radius2,
                    q.x+nextTangent.z*radius2,q.z-nextTangent.x*radius2,q.x+nextTangent.z*radius1,q.z-nextTangent.x*radius1,p.y,q.y);
                p=q; tangent=nextTangent;
            }
        } finally { if(old==null) FRAME.remove(); else FRAME.set(old); }
    }
    private static NodeSettings orient(NodeSettings v,Position pos,Vector tangent) {
        var level=net.minecraft.client.Minecraft.getInstance().level;
        if(level==null) return v;
        var state=level.getBlockState(BlockPos.of(key(pos)));
        if (!(state.getBlock() instanceof org.mtr.mod.block.BlockNode)) return v;
        double angle=Math.toRadians(org.mtr.mod.block.BlockNode.getAngle(new org.mtr.mapping.holder.BlockState(state)));
        double sign=tangent.x*Math.cos(angle)+tangent.z*Math.sin(angle)<0?-1:1;
        return new NodeSettings(v.x(),v.y(),v.z(),v.rotation(),v.cant()*sign);
    }
    private static long key(Position p) { return BlockPos.asLong((int)p.getX(),(int)p.getY(),(int)p.getZ()); }
    private static double distance(Position p,Vector v) { return Math.pow(p.getX()+0.5-v.x,2)+Math.pow(p.getY()-v.y,2)+Math.pow(p.getZ()+0.5-v.z,2); }
}
