package org.mtroptional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;

public final class NodeData extends SavedData {
    public final Map<Long, NodeHistory> nodes = new HashMap<>();
    public static NodeData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(NodeData::load, NodeData::new, "mtr_optional_rail_nodes");
    }
    public static CompoundTag writeValue(NodeSettings value) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("x", value.x()); tag.putDouble("y", value.y()); tag.putDouble("z", value.z());
        tag.putDouble("rotation", value.rotation()); tag.putDouble("cant", value.cant());
        return tag;
    }
    public static NodeSettings readValue(CompoundTag tag) {
        return new NodeSettings(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"), tag.getDouble("rotation"), tag.getDouble("cant"));
    }
    public static NodeData load(CompoundTag tag) {
        NodeData data = new NodeData();
        ListTag list = tag.getList("nodes", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            NodeHistory history = new NodeHistory();
            history.current = readValue(entry.getCompound("value"));
            history.revision = entry.getLong("revision");
            for (int row = 0; row < 3; row++) if (entry.contains("undo" + row)) history.previous[row] = readValue(entry.getCompound("undo" + row));
            data.nodes.put(entry.getLong("pos"), history);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        nodes.forEach((pos, history) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", pos); entry.putLong("revision", history.revision);
            entry.put("value", writeValue(history.current));
            for (int row = 0; row < 3; row++) if (history.previous[row] != null) entry.put("undo" + row, writeValue(history.previous[row]));
            list.add(entry);
        });
        tag.put("nodes", list);
        return tag;
    }
}
