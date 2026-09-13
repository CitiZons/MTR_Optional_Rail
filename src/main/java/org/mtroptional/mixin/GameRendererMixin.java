package org.mtroptional.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.client.VehicleRidingMovement;
import org.mtr.mod.data.VehicleExtension;
import org.mtroptional.OptionalRail;
import org.mtroptional.client.RailTiltClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the same roll to the first-person camera while riding an MTR vehicle. */
@Mixin(value=GameRenderer.class, remap=true)
public abstract class GameRendererMixin {
    @Inject(method="renderLevel", at=@At("HEAD"))
    private void optional$cameraRoll(float partialTick, long limitTime, PoseStack poseStack, CallbackInfo callback) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        for (VehicleExtension vehicle : MinecraftClientData.getInstance().vehicles) {
            if (!VehicleRidingMovement.isRiding(vehicle.getId())) continue;
            double forwardX = Math.sin(Math.toRadians(client.player.getYRot()));
            double forwardZ = -Math.cos(Math.toRadians(client.player.getYRot()));
            double roll = RailTiltClient.getSignedCant(client.player.getX(), client.player.getY(), client.player.getZ(), forwardX, forwardZ);
            if (Math.abs(roll) > .001) poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) roll));
            break;
        }
    }
}
