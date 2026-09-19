package org.mtroptional.probe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import org.mtr.mod.screen.RailModifierScreen;

@Mod("optional_rail_runtime_probe")
public final class RuntimeProbe {
    private int ticks;
    private boolean opened;
    public RuntimeProbe() { MinecraftForge.EVENT_BUS.addListener(this::tick); }
    private void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getInstance();
        if(!opened && mc.screen instanceof TitleScreen && mc.getOverlay()==null) {
            try {
                Class.forName("org.mtr.mod.render.RenderRails");
                Class.forName("org.mtr.mod.render.RenderVehicles");
                Class.forName("org.mtr.mod.render.MainRenderer");
                mc.hitResult=new BlockHitResult(new Vec3(.5,64,.5),Direction.UP,new BlockPos(0,64,0),false);
                mc.setScreen(new RailModifierScreen("runtime-probe"));
                opened=true;
                System.out.println("OPTIONAL_RAIL_PROBE: MTR and renderer mixins loaded; editor opened");
            } catch(Throwable error) { error.printStackTrace(); mc.stop(); }
        } else if(opened && ++ticks==40) {
            Screenshot.grab(mc.gameDirectory,"optional-rail-editor.png",mc.getMainRenderTarget(),message -> System.out.println("OPTIONAL_RAIL_PROBE: "+message.getString()));
        } else if(opened && ticks==80) { System.out.println("OPTIONAL_RAIL_PROBE: PASS"); mc.stop(); }
    }
}
