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
        System.out.println("PASS: "+checks+" checks (ranges, independent Undo, geometry, tangent rotation, cant and gauge)");
    }
}
