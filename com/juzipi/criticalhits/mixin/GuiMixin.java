package com.juzipi.criticalhits.mixin;

import com.juzipi.criticalhits.CriticalHitsClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HUD渲染Mixin，用于在屏幕上绘制武器装备状态提示。
 * 26.2中渲染逻辑从Gui.render移到了Hud.extractRenderState。
 */
@Mixin(Hud.class)
public class GuiMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void criticalhits_onRenderHud(GuiGraphicsExtractor gfx, DeltaTracker deltaTracker, CallbackInfo ci) {
        CriticalHitsClient.renderHud(gfx);
    }
}
