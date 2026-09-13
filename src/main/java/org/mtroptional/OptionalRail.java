package org.mtroptional;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import org.mtr.mod.block.BlockNode;

@Mod(OptionalRail.ID)
public final class OptionalRail {
    public static final String ID = "mtr_optional_rail_addon";
    public OptionalRail() {
        RailNetwork.init();
        MinecraftForge.EVENT_BUS.addListener(this::login);
        MinecraftForge.EVENT_BUS.addListener(this::dimension);
        MinecraftForge.EVENT_BUS.addListener(this::respawn);
        MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST, this::broken);
    }
    private void login(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player) RailNetwork.full(player); }
    private void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { if (event.getEntity() instanceof ServerPlayer player) RailNetwork.full(player); }
    private void respawn(PlayerEvent.PlayerRespawnEvent event) { if (event.getEntity() instanceof ServerPlayer player) RailNetwork.full(player); }
    private void broken(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level && event.getState().getBlock() instanceof BlockNode) {
            NodeData data = NodeData.get(level);
            if (data.nodes.remove(event.getPos().asLong()) != null) {
                data.setDirty();
                RailNetwork.broadcast(level, event.getPos().asLong(), new NodeHistory());
            }
        }
    }
}
