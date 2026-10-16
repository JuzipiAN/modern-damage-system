package com.juzipi.criticalhits.fabric;

/**
 * Mod Menu 集成（可选依赖）。
 *
 * 玩家装了 Mod Menu 时，模组列表里会出现齿轮按钮，可直接打开 MDS 配置界面。
 * 没装 Mod Menu 也不影响：这个类只会被 Mod Menu 的入口点加载。
 */
public class CriticalHitsModFabricModMenu
        implements com.terraformersmc.modmenu.api.ModMenuApi {

    @Override
    public com.terraformersmc.modmenu.api.ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> com.juzipi.criticalhits.MdsConfigGui.create(parent);
    }
}
