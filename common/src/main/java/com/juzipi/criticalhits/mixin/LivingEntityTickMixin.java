package com.juzipi.criticalhits.mixin;

import com.juzipi.criticalhits.CriticalHitsEnchantments;
import com.juzipi.criticalhits.ShieldManager;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * 生物tick Mixin。
 * 每tick更新护盾状态（脱战恢复），并在客户端渲染护盾气泡粒子效果。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTickMixin {

    private static final Random SHIELD_PARTICLE_RANDOM = new Random();
    private int shieldParticleTick = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void criticalhits_onTick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // 检查是否有护盾附魔
        int shieldLevel = CriticalHitsEnchantments.getShieldLevel(entity);
        if (shieldLevel > 0) {
            float maxShield = CriticalHitsEnchantments.calculateShieldValue(shieldLevel, entity.getMaxHealth());
            ShieldManager.setMaxShield(entity, maxShield);

            // 客户端：渲染护盾气泡粒子效果
            if (entity.level().isClientSide()) {
                renderShieldParticles(entity, shieldLevel);
            } else {
                // 计算恢复时间（服务器统一10秒，单人按难度）
                int regenTicks = 200; // 默认10秒
                try {
                    // 检查是否为单人游戏
                    var server = entity.level().getServer();
                    if (server != null && server.isSingleplayer()) {
                        // 单人游戏按难度
                        var difficulty = entity.level().getDifficulty();
                        switch (difficulty) {
                            case PEACEFUL, EASY -> regenTicks = 100; // 5秒
                            case NORMAL -> regenTicks = 160; // 8秒
                            case HARD -> regenTicks = 260; // 13秒
                        }
                    }
                } catch (Exception e) {
                    // 忽略异常，使用默认值
                }

                ShieldManager.tick(entity, regenTicks);
            }
        } else {
            // 没有护盾附魔，清除护盾数据
            ShieldManager.setMaxShield(entity, 0);
        }
    }

    /**
     * 渲染护盾保护罩粒子效果（客户端）。
     * 在实体周围生成一个柔和的半透明保护罩，使用CLOUD粒子，缓慢旋转，不刺眼。
     */
    private void renderShieldParticles(LivingEntity entity, int shieldLevel) {
        shieldParticleTick++;
        // 每2 tick生成一次粒子，形成密集的保护罩轮廓
        if (shieldParticleTick % 2 != 0) return;

        double entityWidth = entity.getBbWidth();
        double entityHeight = entity.getBbHeight();
        double centerX = entity.getX();
        double centerY = entity.getY() + entityHeight / 2.0;
        double centerZ = entity.getZ();

        // 保护罩半径（比实体稍大，围住整个实体）
        double radius = Math.max(entityWidth, entityHeight * 0.6) * 1.3 + 0.3;

        // 使用球面均匀分布生成粒子，形成完整的保护罩轮廓
        // 每次生成10-16个粒子，分布在球面上，形成罩子的轮廓
        int particleCount = 10 + shieldLevel * 2;
        // 白色半透明DUST粒子，大小0.4（颜色为RGB整数）
        DustParticleOptions shieldParticle = new DustParticleOptions(0xE6F2FF, 0.4f);

        for (int i = 0; i < particleCount; i++) {
            // 球面均匀分布（使用黄金角分布）
            double phi = Math.acos(1 - 2 * (i + 0.5) / particleCount);
            double theta = Math.PI * (1 + Math.sqrt(5)) * i;
            // 加上缓慢旋转的时间偏移
            double rotationOffset = shieldParticleTick * 0.005;
            theta += rotationOffset;

            double px = centerX + radius * Math.sin(phi) * Math.cos(theta);
            double py = centerY + radius * Math.cos(phi);
            double pz = centerZ + radius * Math.sin(phi) * Math.sin(theta);

            // 粒子几乎不移动，只做微小的浮动，形成稳定的保护罩
            double vx = (SHIELD_PARTICLE_RANDOM.nextDouble() - 0.5) * 0.002;
            double vy = (SHIELD_PARTICLE_RANDOM.nextDouble() - 0.5) * 0.002;
            double vz = (SHIELD_PARTICLE_RANDOM.nextDouble() - 0.5) * 0.002;

            // 使用白色半透明DUST粒子，形成完整的保护罩气泡效果
            entity.level().addParticle(
                shieldParticle,
                px, py, pz,
                vx, vy, vz
            );
        }
    }
}
