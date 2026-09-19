package org.mtroptional.mixin;

import org.mtr.core.data.Rail;
import org.mtr.core.data.RailMath;
import org.mtr.mapping.holder.*;
import org.mtr.mod.render.RenderRails;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.resource.RailResource;
import org.mtroptional.client.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=RenderRails.class,remap=false)
public abstract class RenderRailsMixin {
    @Redirect(method="renderWithinRenderDistance",at=@At(value="INVOKE",target="Lorg/mtr/core/data/RailMath;render(Lorg/mtr/core/data/RailMath$RenderRail;DFF)V"))
    private static void optional$geometry(RailMath math,RailMath.RenderRail callback,double interval,float r1,float r2,Rail rail,@Coerce Object ignored,double outerInterval,float outerR1,float outerR2) {
        RailGeometry.render(rail,callback,interval,r1,r2);
    }
    @Redirect(method="lambda$renderRailStandard$16",at=@At(value="INVOKE",target="Lorg/mtr/mod/resource/RailResource;render(Lorg/mtr/mod/render/StoredMatrixTransformations;I)V"))
    private static void optional$model(RailResource resource,StoredMatrixTransformations transform,int light,ClientWorld world,RailResource ignored,boolean flip,boolean[] types,BlockPos pos,double x1,double z1,double x2,double z2,double x3,double z3,double x4,double z4,double y1,double y2) {
        RailGeometry.Frame frame=RailGeometry.FRAME.get();
        if(frame!=null) {
            transform=transform.copy();
            // MTR first rotates model X by PI, reversing its local vertical axis.
            float roll=(float)(-(frame.cantA()+frame.cantB())/2*(flip?-1:1));
            // Roll about the same path centre as vehicles, not the elevated model origin.
            double pivot = resource.getModelYOffset();
            transform.add(graphics -> {
                graphics.translate(0, pivot, 0);
                graphics.rotateZDegrees(roll);
                graphics.translate(0, -pivot, 0);
            });
        }
        // Models are already rolled in their local coordinate system; do not bank their textures again.
        RailGeometry.FRAME.remove();
        try { resource.render(transform,light); }
        finally { if(frame!=null) RailGeometry.FRAME.set(frame); }
    }
    @Redirect(method="renderNode",at=@At(value="NEW",target="(DDD)Lorg/mtr/mod/render/StoredMatrixTransformations;"))
    private static StoredMatrixTransformations optional$node(double x,double y,double z,BlockState state,BlockPos pos,java.util.function.BooleanSupplier shouldRender,int light) {
        var value=ClientNodes.get(net.minecraft.core.BlockPos.asLong(pos.getX(),pos.getY(),pos.getZ()));
        StoredMatrixTransformations transform=new StoredMatrixTransformations(x+value.x(),y+value.y(),z+value.z());
        transform.add(graphics -> graphics.rotateYDegrees((float)-value.rotation()));
        return transform;
    }
}
