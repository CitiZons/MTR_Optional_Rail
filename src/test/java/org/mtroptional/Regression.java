package org.mtroptional;

import org.mtr.core.data.*;
import org.mtr.core.tool.Angle;
import org.mtr.core.tool.Vector;
import org.mtroptional.client.Curve;
import org.mtroptional.client.RailGeometry;

public final class Regression {
    private static int checks;
    private static void check(boolean ok,String message) { checks++; if(!ok) throw new AssertionError(message); }
    private static void near(double expected,double actual,String message) { check(Math.abs(expected-actual)<0.0001,message+": "+actual); }
    public static void main(String[] args) {
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,1.01,-1.01}) {
            try { new NodeSettings(bad,0,0,0,0); throw new AssertionError("Accepted invalid offset"); } catch(IllegalArgumentException expected) { checks++; }
        }
        near(0.01,new NodeSettings(0.011,0,0,0,0).x(),"Hundredths");
        NodeHistory history=new NodeHistory();
        NodeSettings first=new NodeSettings(.25,-.5,1,30,15),second=new NodeSettings(.75,.5,-1,-30,-15);
        history.edit(0,false,first); history.edit(1,false,first); history.edit(2,false,first);
        check(history.current.equals(first),"Independent row apply");
        history.edit(0,false,second); history.edit(0,true,NodeSettings.ZERO);
        check(history.current.equals(first),"Undo exactly the previous apply");
        check(!history.edit(0,true,NodeSettings.ZERO),"Undo does not toggle redo");
        history.edit(1,true,NodeSettings.ZERO);
        near(15,history.current.cant(),"Rotation undo preserves cant");
        near(.25,history.current.x(),"Rotation undo preserves offset");
        check(!history.edit(2,false,first),"No-op apply retains undo");
        check((history.undoMask()&4)!=0,"Cant undo survives no-op");

        RailMath math=new RailMath(new Position(0,64,0),Angle.fromAngle(0),new Position(20,64,0),Angle.fromAngle(180),Rail.Shape.QUADRATIC,0);
        Curve base=new Curve(math,NodeSettings.ZERO,NodeSettings.ZERO);
        for(int i=0;i<=20;i++) {
            Vector expected=math.getPosition(math.getLength()*i/20,false),actual=base.point(i/20.0);
            near(expected.x,actual.x,"Base curve x"); near(expected.y,actual.y,"Base curve y"); near(expected.z,actual.z,"Base curve z");
        }
        Curve moved=new Curve(math,first,second);
        near(base.point(0).x+.25,moved.point(0).x,"Start offset");
        near(base.point(1).z-1,moved.point(1).z,"End offset");
        near(base.point(0).y-.5,moved.point(0).y,"Start elevation");
        Curve rotated=new Curve(math,new NodeSettings(0,0,0,90,0),NodeSettings.ZERO);
        check(rotated.tangent(0).z>.9999,"Rotation changes endpoint tangent by 90 degrees");
        near(base.point(1).x,rotated.point(1).x,"Rotation retains opposite endpoint");
        near(0,moved.cant(.5),"Cant smoothly interpolates opposite endpoint values");
        RailGeometry.Frame frame=new RailGeometry.Frame(new Vector(0,0,0),new Vector(10,0,0),new Vector(1,0,0),new Vector(1,0,0),45,45);
        Vector left=frame.bank(0,0,-1),right=frame.bank(0,0,1);
        near(Math.sqrt(.5),left.y,"Left rail raised"); near(-Math.sqrt(.5),right.y,"Right rail lowered");
        near(2,left.distanceTo(right),"Cant preserves gauge");
        near(left.y,frame.bank(0,0,-1).y,"Vertex order independent");
        // Former nearest-point lookup jumps in metre-sized steps, including on long rails.
        for (int length : new int[]{20, 500}) {
            RailMath straight = new RailMath(new Position(0,64,0),Angle.fromAngle(0),new Position(length,64,0),Angle.fromAngle(180),Rail.Shape.QUADRATIC,0);
            var projection = new org.mtroptional.client.RailProjection(straight);
            for (int i=0; i<100; i++) {
                double t=.4+i*.0001;
                Vector point=straight.getPosition(straight.getLength()*t,false);
                near(t,projection.project(point.x,point.y,point.z).progress(),"Sub-sample continuous progress");
            }
        }
        // Model Y offsets must rotate with the rail, matching vehicle and texture banking.
        for (double degrees : new double[]{-45,-20,0,20,45}) {
            double angle=Math.toRadians(degrees), offset=.25;
            RailGeometry.Frame banked=new RailGeometry.Frame(new Vector(0,0,0),new Vector(0,0,10),new Vector(0,0,1),new Vector(0,0,1),degrees,degrees);
            for (double wheelX : new double[]{-.75,.75}) {
                Vector expected=banked.bank(wheelX,offset,0);
                // T(offset) * Rx(PI) * T(pivot) * Rz(-cant) * T(-pivot).
                double localY=-offset;
                double modelX=wheelX*Math.cos(-angle)-localY*Math.sin(-angle);
                double modelY=offset-(offset+wheelX*Math.sin(-angle)+localY*Math.cos(-angle));
                near(expected.x,modelX,"Rail model lateral contact matches banked wheel plane");
                near(expected.y,modelY,"Both rail heads match banked wheel plane");
            }
        }
        // Reproduce the reported setup: both endpoints raised 0.5 blocks, cant 30 degrees.
        NodeSettings raised=new NodeSettings(0,.5,0,0,30);
        Curve raisedRail=new Curve(math,raised,raised);
        for (double t : new double[]{0,.25,.5,.75,1}) {
            double lift=Curve.verticalOffset(raised,raised,t);
            near(.5,lift,"Vehicle receives authored half-block lift");
            near(raisedRail.point(t).y,base.point(t).y+lift,"Vehicle and rail share elevation");
            for (double side : new double[]{-.75,.75}) {
                // A lower wheel contact point and its rail contact point must retain their gap.
                double contact=.03, angle=Math.toRadians(30);
                double wheel=base.point(t).y+lift+side*Math.sin(angle)+contact*Math.cos(angle);
                double rail=raisedRail.point(t).y+side*Math.sin(angle)+contact*Math.cos(angle);
                near(rail,wheel,"Both lower wheel contacts follow raised banked rail");
            }
            near(0,Curve.verticalOffset(NodeSettings.ZERO,NodeSettings.ZERO,t),"Unedited track has no lift");
            near(moved.point(t).y-base.point(t).y,Curve.verticalOffset(first,second,t),"Changing heights use rail interpolation");
        }
        // XYZ trajectory correction must equal the rendered rail displacement everywhere,
        // including endpoint tangent rotations and travel in either direction.
        for (Curve curve : new Curve[]{moved,rotated,new Curve(math,new NodeSettings(.5,.5,-.75,0,30),new NodeSettings(.5,.5,-.75,0,30))}) {
            for (int i=0;i<=40;i++) {
                double t=i/40D;
                Vector original=math.getPosition(math.getLength()*t,false);
                Vector corrected=original.add(curve.offset(t));
                Vector rail=curve.point(t);
                near(rail.x,corrected.x,"Vehicle follows rail X");
                near(rail.y,corrected.y,"Vehicle follows rail Y");
                near(rail.z,corrected.z,"Vehicle follows rail Z");
            }
        }
        Vector common=moved.offset(.4);
        Vector relative=common.add(common.multiply(-1,-1,-1)).rotateY(Math.PI/2);
        near(0,relative.x,"Common riding X offset cancels");
        near(0,relative.y,"Common riding Y offset cancels");
        near(0,relative.z,"Common riding Z offset cancels");
        System.out.println("PASS: "+checks+" checks (ranges, independent Undo, geometry, tangent rotation, cant and gauge)");
    }
}
