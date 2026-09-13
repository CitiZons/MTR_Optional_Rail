package org.mtroptional.mixin;

import org.mtr.mod.client.IDrawing;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtroptional.client.RailGeometry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=org.mtr.mod.render.RenderRails.class,remap=false)
public abstract class DrawingMixin {
    @Redirect(method={"lambda$renderRailStandard$18","lambda$renderSignalsStandard$20","lambda$renderRailOneWayArrows$13"},at=@At(value="INVOKE",target="Lorg/mtr/mod/client/IDrawing;drawTexture(Lorg/mtr/mapping/mapper/GraphicsHolder;DDDDDDDDDDDDLorg/mtr/mapping/holder/Vector3d;FFFFLorg/mtr/mapping/holder/Direction;II)V"))
    private static void optional$bank(GraphicsHolder graphics,double x1,double y1,double z1,double x2,double y2,double z2,double x3,double y3,double z3,double x4,double y4,double z4,Vector3d offset,float u1,float v1,float u2,float v2,Direction facing,int color,int light) {
        RailGeometry.Frame frame=RailGeometry.FRAME.get();
        if(frame==null || (frame.cantA()==0 && frame.cantB()==0)) {
            IDrawing.drawTexture(graphics,x1,y1,z1,x2,y2,z2,x3,y3,z3,x4,y4,z4,offset,u1,v1,u2,v2,facing,color,light);
            return;
        }
        var a=frame.bank(x1,y1,z1); var b=frame.bank(x2,y2,z2); var c=frame.bank(x3,y3,z3); var d=frame.bank(x4,y4,z4);
        RailGeometry.FRAME.remove();
        try { IDrawing.drawTexture(graphics,a.x,a.y,a.z,b.x,b.y,b.z,c.x,c.y,c.z,d.x,d.y,d.z,offset,u1,v1,u2,v2,facing,color,light); }
        finally { RailGeometry.FRAME.set(frame); }
    }
}
