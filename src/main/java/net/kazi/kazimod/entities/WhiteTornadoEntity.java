package net.kazi.kazimod.entities;

import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.kazi.kazimod.particles.GreenTornadoParticleEffect;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.entities.TornadoEntity;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class WhiteTornadoEntity extends TornadoEntity {

    // ── Synced color components — visible on both server and client ───────────
    private static final DataParameter<Float> DATA_RED   =
            EntityDataManager.defineId(WhiteTornadoEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> DATA_GREEN =
            EntityDataManager.defineId(WhiteTornadoEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> DATA_BLUE  =
            EntityDataManager.defineId(WhiteTornadoEntity.class, DataSerializers.FLOAT);

    private GreenTornadoParticleEffect.Details details = new GreenTornadoParticleEffect.Details();

    public WhiteTornadoEntity(World level, LivingEntity entity) {
        super(level, entity);
    }

    public WhiteTornadoEntity(EntityType<?> type, World pLevel) {
        super(type, pLevel);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_RED,   1.0F);
        this.entityData.define(DATA_GREEN, 1.0F);
        this.entityData.define(DATA_BLUE,  1.0F);
    }

    @Override
    public EntityType<?> getType() {
        return KaziEntities.WHITE_TORNADO.get();
    }

    // ── Color API — synced automatically via EntityDataManager ───────────────
    public void setColor(float r, float g, float b) {
        this.entityData.set(DATA_RED,   r);
        this.entityData.set(DATA_GREEN, g);
        this.entityData.set(DATA_BLUE,  b);
    }

    public float getRed()   { return this.entityData.get(DATA_RED);   }
    public float getGreen() { return this.entityData.get(DATA_GREEN); }
    public float getBlue()  { return this.entityData.get(DATA_BLUE);  }

    @Override
    public void tick() {
        super.tick();

        if (!this.level.isClientSide) {
            // Spiral green tornado particles every 40 ticks
            if (this.tickCount % 40 == 0) {
                this.details.setSize(this.getSize());
                WyHelper.spawnParticleEffect(
                        (ParticleEffect) KaziParticleEffects.GREEN_TORNADO.get(),
                        this,
                        this.getX(), this.getY(), this.getZ(),
                        this.details
                );
            }

            // Sweep particles every 2 ticks for dense coverage
            if (this.tickCount % 2 == 0 && this.level instanceof ServerWorld) {
                float size = this.getSize();

                float visualHeight    = size * 3.0F;
                double minRadius      = size / 7.0F;
                double maxRadius      = size * 1.5F;
                double radiusIncrement = maxRadius / visualHeight;

                for (double y = 0.0; y < visualHeight; y += 0.3) {
                    double radius = Math.max(y * radiusIncrement, minRadius);
                    for (int i = 0; i < 8; i++) {
                        double offsetAngle = (360.0 / 8.0 * i) + y * 20.0 + this.tickCount * 8.0;
                        double sx = Math.cos(Math.toRadians(offsetAngle)) * radius;
                        double sz = Math.sin(Math.toRadians(offsetAngle)) * radius;
                    }
                }
            }
        }
    }
}