package org.mtroptional;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import org.mtr.mod.block.BlockNode;
import org.mtr.mod.item.ItemBrush;
import org.mtr.mod.packet.PacketUpdateData;
import org.mtr.core.data.Position;
import org.mtr.core.data.Rail;
import org.mtr.core.tool.Utilities;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.function.Supplier;

public final class RailNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(OptionalRail.ID, "nodes"), () -> "1", "1"::equals, "1"::equals);
    public record Edit(long pos, int row, boolean undo, long revision, double x, double y, double z, double rotation, double cant) {}
    public record State(String dimension, boolean clear, long pos, NodeSettings value, long revision, int undoMask) {}
    public static void init() {
        CHANNEL.messageBuilder(Edit.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b) -> { b.writeLong(m.pos); b.writeByte(m.row); b.writeBoolean(m.undo); b.writeLong(m.revision); b.writeDouble(m.x); b.writeDouble(m.y); b.writeDouble(m.z); b.writeDouble(m.rotation); b.writeDouble(m.cant); })
            .decoder(b -> new Edit(b.readLong(), b.readByte(), b.readBoolean(), b.readLong(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble()))
            .consumerMainThread(RailNetwork::edit).add();
        CHANNEL.messageBuilder(State.class, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b) -> { b.writeUtf(m.dimension); b.writeBoolean(m.clear); b.writeLong(m.pos); write(b,m.value); b.writeLong(m.revision); b.writeByte(m.undoMask); })
            .decoder(b -> new State(b.readUtf(), b.readBoolean(), b.readLong(), read(b), b.readLong(), b.readByte()))
            .consumerMainThread((m,c) -> org.mtroptional.client.ClientNodes.receive(m)).add();
    }
    private static void write(FriendlyByteBuf b, NodeSettings v) { b.writeDouble(v.x()); b.writeDouble(v.y()); b.writeDouble(v.z()); b.writeDouble(v.rotation()); b.writeDouble(v.cant()); }
    private static NodeSettings read(FriendlyByteBuf b) { return new NodeSettings(b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble()); }
    public static void send(Edit edit) { CHANNEL.sendToServer(edit); }
    private static void edit(Edit edit, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player == null || edit.row < 0 || edit.row > 2) return;
        ServerLevel level = player.serverLevel();
        BlockPos pos = BlockPos.of(edit.pos);
        if (!level.hasChunkAt(pos) || player.distanceToSqr(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5) > 64
            || !level.mayInteract(player, pos) || !player.mayBuild()
            || !(level.getBlockState(pos).getBlock() instanceof BlockNode)
            || !(player.getMainHandItem().getItem() instanceof ItemBrush || player.getOffhandItem().getItem() instanceof ItemBrush)) return;
        NodeData data = NodeData.get(level);
        NodeHistory history = data.nodes.computeIfAbsent(edit.pos, ignored -> new NodeHistory());
        if (history.revision != edit.revision) { send(player, state(level, edit.pos, history)); return; }
        try {
            NodeSettings value = new NodeSettings(edit.x, edit.y, edit.z, edit.rotation, edit.cant);
            if (history.edit(edit.row, edit.undo, value)) { data.setDirty(); broadcast(level, edit.pos, history); }
            else send(player, state(level, edit.pos, history));
        } catch (IllegalArgumentException ignored) { send(player, state(level, edit.pos, history)); }
    }
    private static org.mtr.core.data.Data serverData(ServerLevel level) {
        try {
            java.lang.reflect.Field mainField = org.mtr.mod.Init.class.getDeclaredField("main"); mainField.setAccessible(true);
            Object main = mainField.get(null);
            java.lang.reflect.Field simulatorsField = org.mtr.core.Main.class.getDeclaredField("simulators"); simulatorsField.setAccessible(true);
            for (Object simulator : (java.util.Collection<?>) simulatorsField.get(main)) {
                if (simulator instanceof org.mtr.core.simulation.Simulator data && data.dimension.equals(level.dimension().location().toString())) return data;
            }
        } catch (ReflectiveOperationException ignored) { }
        return null;
    }
    private static State state(ServerLevel level, long pos, NodeHistory history) { return new State(level.dimension().location().toString(), false, pos, history.current, history.revision, history.undoMask()); }
    private static void send(ServerPlayer player, State state) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), state); }
    public static void broadcast(ServerLevel level, long pos, NodeHistory history) { CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), state(level, pos, history)); }
    public static void full(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        send(player, new State(level.dimension().location().toString(), true, 0, NodeSettings.ZERO, 0, 0));
        NodeData.get(level).nodes.forEach((pos,history) -> send(player, state(level,pos,history)));
    }
}



