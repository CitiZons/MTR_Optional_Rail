package org.mtroptional.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mtr.mod.screen.RailModifierScreen;
import org.mtroptional.*;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = OptionalRail.ID, value = Dist.CLIENT)
public final class EditorEvents {
    public static final int HEIGHT = 88;
    private static final Map<Screen, Panel> PANELS = new WeakHashMap<>();
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { PANELS.clear(); ClientNodes.clear(); }
    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof RailModifierScreen)) return;
        Screen screen = event.getScreen();
        Panel old = PANELS.get(screen);
        BlockPos pos = old == null ? Minecraft.getInstance().hitResult instanceof BlockHitResult hit ? hit.getBlockPos() : null : old.pos;
        if (pos == null) return;
        for (var child : event.getListenersList()) if (child instanceof AbstractWidget widget) widget.setY(widget.getY() + HEIGHT);
        Panel panel = new Panel(pos);
        PANELS.put(screen, panel);
        int available = Math.min(540, screen.width - 8);
        int label = Math.min(98, available / 4);
        int buttonWidth = Math.min(54, available / 7);
        int end = available - 2 * buttonWidth - 6;
        for (int row = 0; row < 3; row++) {
            int y = 4 + row * 25;
            int count = row == 0 ? 3 : 1;
            for (int index = 0; index < count; index++) {
                int slot = row == 0 ? index : row + 2;
                int w = (end - label) / count;
                EditBox box = new EditBox(Minecraft.getInstance().font, label + index*w, y, w-3, 20, Component.literal(slot < 3 ? "XYZ".substring(slot,slot+1) : row == 1 ? "Rotation" : "Cant"));
                box.setMaxLength(10);
                box.setFilter(text -> text.matches("-?\\d{0,3}(\\.\\d{0,2})?"));
                box.setHint(Component.literal(slot < 3 ? "XYZ".substring(slot,slot+1) : "0.00"));
                panel.fields[slot] = box;
                box.setResponder(text -> panel.validate());
                event.addListener(box);
            }
            final int selectedRow = row;
            panel.apply[row] = Button.builder(Component.literal("Apply"), button -> panel.submit(selectedRow,false)).bounds(end+2,y,buttonWidth,20).build();
            panel.undo[row] = Button.builder(Component.literal("Undo"), button -> panel.submit(selectedRow,true)).bounds(end+buttonWidth+5,y,buttonWidth,20).build();
            event.addListener(panel.apply[row]); event.addListener(panel.undo[row]);
        }
        panel.refresh();
    }
    @SubscribeEvent public static void draw(ScreenEvent.Render.Post event) {
        Panel panel = PANELS.get(event.getScreen());
        if (panel == null) return;
        var graphics = event.getGuiGraphics();
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("mtroptional.offset"), 4,10,0xFFFFFF,false);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("mtroptional.rotation"), 4,35,0xFFFFFF,false);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("mtroptional.cant"), 4,60,0xFFFFFF,false);
                graphics.drawString(Minecraft.getInstance().font, Component.translatable(panel.pending ? "mtroptional.wait" : "mtroptional.ranges"), 4,78,0xBBBBBB,false);
        if (panel.pending && System.currentTimeMillis() - panel.sentAt > 3000) { panel.pending = false; panel.validate(); }
    }
    public static void updated(RailNetwork.State state) {
        Panel panel = PANELS.get(Minecraft.getInstance().screen);
        if (panel != null && !state.clear() && panel.pos.asLong() == state.pos()) panel.refresh();
    }
    private static final class Panel {
        final BlockPos pos;
        final EditBox[] fields = new EditBox[5];
        final Button[] apply = new Button[4], undo = new Button[4];
        boolean pending;
        long sentAt;
        Panel(BlockPos pos) { this.pos = pos; }
        void refresh() {
            pending = false;
            NodeSettings value = ClientNodes.get(pos.asLong());
            var state = ClientNodes.state(pos.asLong());
            double[] values = {value.x(),value.y(),value.z(),value.rotation(),value.cant()};
            for (int i=0;i<5;i++) fields[i].setValue(i == 5 ? String.format(Locale.ROOT,"%.0f",values[i]) : String.format(Locale.ROOT,"%.2f",values[i]));
            validate();
        }
        double parse(int index) { return index == 5 ? Math.max(0, Math.min(400, Double.parseDouble(fields[index].getValue()))) : NodeSettings.validate(Double.parseDouble(fields[index].getValue()),index<3?1:index==3?90:45); }
        void validate() {
            for (int row=0;row<4;row++) {
                if (apply[row] == null) continue;
                boolean valid = true;
                for (int i=row==0?0:row+2;i<(row==0?3:row+3);i++) {
                    try { parse(i); fields[i].setTextColor(0xFFFFFF); }
                    catch (IllegalArgumentException ex) { valid=false; fields[i].setTextColor(0xFF6666); }
                }
                apply[row].active = valid && !pending;
                undo[row].active = !pending && (ClientNodes.state(pos.asLong()).undoMask() & (1<<row)) != 0;
            }
        }
        void submit(int row, boolean undoAction) {
            var state = ClientNodes.state(pos.asLong());
            NodeSettings v = state.value();
            try {
                if (!undoAction) v = switch(row) {
                    case 0 -> new NodeSettings(parse(0),parse(1),parse(2),v.rotation(),v.cant());
                    case 1 -> new NodeSettings(v.x(),v.y(),v.z(),parse(3),v.cant());
                    default -> new NodeSettings(v.x(),v.y(),v.z(),v.rotation(),parse(4));
                };
                
                pending=true; sentAt=System.currentTimeMillis(); validate();
                RailNetwork.send(new RailNetwork.Edit(pos.asLong(),row,undoAction,state.revision(),v.x(),v.y(),v.z(),v.rotation(),v.cant()));
            } catch (IllegalArgumentException ex) { validate(); }
        }
    }
}
