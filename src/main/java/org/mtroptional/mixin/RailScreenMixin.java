package org.mtroptional.mixin;

import org.mtr.mod.screen.RailModifierScreen;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.holder.MutableText;
import org.mtroptional.client.EditorEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value=RailModifierScreen.class, remap=false)
public abstract class RailScreenMixin {
    @Redirect(method="render", at=@At(value="INVOKE",target="Lorg/mtr/mapping/mapper/GraphicsHolder;drawText(Lorg/mtr/mapping/holder/MutableText;IIIZI)V"))
    private void optional$label(GraphicsHolder graphics, MutableText text, int x, int y, int color, boolean shadow, int light) {
        graphics.drawText(text,x,y+EditorEvents.HEIGHT,color,shadow,light);
    }
    @Redirect(method="render", at=@At(value="INVOKE",target="Lorg/mtr/mapping/mapper/GuiDrawing;drawRectangle(DDDDI)V"))
    private void optional$background(GuiDrawing drawing,double x1,double y1,double x2,double y2,int color) {
        drawing.drawRectangle(x1,y1,x2,y2+EditorEvents.HEIGHT,color);
    }
}
