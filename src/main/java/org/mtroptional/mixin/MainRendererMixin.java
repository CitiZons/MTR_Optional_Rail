package org.mtroptional.mixin;

import org.mtr.mod.render.MainRenderer;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.holder.Vector3d;
import org.mtroptional.client.RailGeometry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import java.util.function.BiConsumer;

@Mixin(value=MainRenderer.class,remap=false)
public abstract class MainRendererMixin {
    @ModifyVariable(method="scheduleRender(Lorg/mtr/mapping/holder/Identifier;ZLorg/mtr/mod/render/QueuedRenderLayer;Ljava/util/function/BiConsumer;)V",at=@At("HEAD"),argsOnly=true)
    private static BiConsumer<GraphicsHolder,Vector3d> optional$capture(BiConsumer<GraphicsHolder,Vector3d> callback) {
        RailGeometry.Frame frame=RailGeometry.FRAME.get();
        if(frame==null) return callback;
        return (graphics,offset) -> {
            RailGeometry.Frame old=RailGeometry.FRAME.get(); RailGeometry.FRAME.set(frame);
            try { callback.accept(graphics,offset); }
            finally { if(old==null) RailGeometry.FRAME.remove(); else RailGeometry.FRAME.set(old); }
        };
    }
}
