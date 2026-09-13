package org.mtroptional.mixin;

import org.mtr.mod.render.PositionAndRotation;
import org.mtr.mod.render.RenderVehicles;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtroptional.client.RailTiltClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=RenderVehicles.class, remap=false)
public abstract class RenderVehiclesMixin {
    @Inject(method="getStoredMatrixTransformations", at=@At("RETURN"), cancellable=true)
    private static void optional$vehicleCant(boolean useOffset, PositionAndRotation position, double oscillation, CallbackInfoReturnable<StoredMatrixTransformations> callback) {
        StoredMatrixTransformations original = callback.getReturnValue();
        if (original == null || position == null || position.position == null) return;
        double forwardX = Math.sin(position.yaw), forwardZ = Math.cos(position.yaw);
        double cant = RailTiltClient.getSignedCant(position.position.x(), position.position.y(), position.position.z(), forwardX, forwardZ);
        if (Math.abs(cant) < .001) return;
        StoredMatrixTransformations transformed = original.copy();
        transformed.add(graphics -> graphics.rotateZDegrees((float) cant));
        callback.setReturnValue(transformed);
    }
}
