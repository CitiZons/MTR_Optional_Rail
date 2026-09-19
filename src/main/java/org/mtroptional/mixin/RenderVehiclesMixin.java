package org.mtroptional.mixin;

import org.mtr.mod.render.PositionAndRotation;
import org.mtr.mod.render.RenderVehicles;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtroptional.client.RailTiltClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=RenderVehicles.class, remap=false)
public abstract class RenderVehiclesMixin {
    @Unique
    private static final java.util.Map<PositionAndRotation, Double> optional$renderCant = new java.util.WeakHashMap<>();

    @Inject(method="getRenderPositionAndRotation", at=@At("RETURN"), cancellable=true)
    private static void optional$offset(org.mtr.mapping.holder.Vector3d offset, Double offsetRotation,
            PositionAndRotation riding, PositionAndRotation absolute,
            org.mtr.mapping.holder.Vector3d shake, CallbackInfoReturnable<PositionAndRotation> callback) {
        PositionAndRotation rendered = callback.getReturnValue();
        if (rendered == null || absolute == null) return;
        var pose = RailTiltClient.getPose(absolute.position.x, absolute.position.y, absolute.position.z,
                Math.sin(absolute.yaw), Math.cos(absolute.yaw));
        org.mtr.core.tool.Vector displacement = pose.offset();
        // MTR renders relative to the riding car; common XYZ displacement must cancel.
        if (offset != null && riding != null) {
            var ridingOffset = RailTiltClient.getPose(riding.position.x, riding.position.y, riding.position.z,
                    Math.sin(riding.yaw), Math.cos(riding.yaw)).offset();
            displacement = displacement.add(ridingOffset.multiply(-1,-1,-1));
            if (offsetRotation != null) {
                // Use exactly the extra yaw applied by MTR's riding-relative render transform.
                displacement = displacement.rotateY(rendered.yaw - absolute.yaw);
            }
        }
        if (Math.abs(displacement.x) + Math.abs(displacement.y) + Math.abs(displacement.z) > 1E-9) {
            rendered = new PositionAndRotation(rendered.position.add(displacement), rendered.yaw, rendered.pitch);
            callback.setReturnValue(rendered);
        }
        optional$renderCant.put(rendered, pose.cant());
    }

    @Inject(method="getStoredMatrixTransformations", at=@At("RETURN"), cancellable=true)
    private static void optional$vehicleCant(boolean useOffset, PositionAndRotation position, double oscillation, CallbackInfoReturnable<StoredMatrixTransformations> callback) {
        StoredMatrixTransformations original = callback.getReturnValue();
        if (original == null || position == null || position.position == null) return;
        double forwardX = Math.sin(position.yaw), forwardZ = Math.cos(position.yaw);
        Double worldCant = optional$renderCant.get(position);
        double cant = worldCant != null ? worldCant : RailTiltClient.getSignedCant(position.position.x(), position.position.y(), position.position.z(), forwardX, forwardZ);
        if (Math.abs(cant) < .001) return;
        StoredMatrixTransformations transformed = original.copy();
        transformed.add(graphics -> graphics.rotateZDegrees((float) cant));
        callback.setReturnValue(transformed);
    }
}
