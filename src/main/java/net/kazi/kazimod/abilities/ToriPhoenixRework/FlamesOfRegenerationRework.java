package net.kazi.kazimod.abilities.ToriPhoenixRework;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.*;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.*;
import xyz.pixelatedw.mineminenomi.packets.server.ability.SUpdatePassiveAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

import java.awt.Color;
import java.text.DecimalFormat;

public class FlamesOfRegenerationRework extends PassiveAbility2 {
    private static final int MAX_ENERGY = 100;
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "flames_of_regeneration", new Pair[]{
            ImmutablePair.of("Protects the user and heals them back up when damage is taken, has an initial reserve of §a%s Energy§r which increases with time and decreses with each heal.", new Object[]{100})
    });
    public static final AbilityCore<FlamesOfRegenerationRework> INSTANCE = new AbilityCore.Builder<FlamesOfRegenerationRework>(
            "Flames of Regeneration", AbilityCategory.DEVIL_FRUITS, AbilityType.PASSIVE, FlamesOfRegenerationRework::new)
            .addDescriptionLine(DESCRIPTION)
            .setIcon(new ResourceLocation("mineminenomi", "textures/abilities/flames_of_regeneration.png"))
            .build();

    private final DamageTakenComponent damageTakenComponent = new DamageTakenComponent(this).addOnAttackEvent(this::onDamageTaken);
    private final ConsumptionComponent consumptionComponent = new ConsumptionComponent(this).addConsumptionEvent(this::onConsumption);
    private static final int MAX_COOLDOWN = 100;
    private double energy = 100.0D;
    private int cooldown;
    private int invulnerableTime;
    private final Interval recuperationInterval = new Interval(10);
    private final Interval regenerationInterval = new Interval(40);

    public FlamesOfRegenerationRework(AbilityCore<FlamesOfRegenerationRework> ability) {
        super(ability);
        if (super.isClientSide()) {
            GaugeComponent gaugeComponent = new GaugeComponent(this, this::renderGauge);
            super.addComponents(gaugeComponent);
        }
        super.addComponents(this.damageTakenComponent, this.consumptionComponent);
        super.addDuringPassiveEvent(this::duringPassive);
    }

    private void duringPassive(LivingEntity entity) {
        if (!entity.level.isClientSide) {
            --this.invulnerableTime;
            if (this.regenerationInterval.canTick() && this.energy - 4.0D >= 0.0D && entity.getHealth() < entity.getMaxHealth()) {
                WyHelper.spawnParticleEffect((ParticleEffect) ModParticleEffects.FLAMES_OF_REGEN.get(), entity, entity.getX(), entity.getY(), entity.getZ());
                entity.heal(4.0F);
                this.addEnergy(entity, -5.0D);
            }
            if (!WyHelper.isInCombat(entity)) {
                if (entity.isSleeping() && this.recuperationInterval.canTick()) {
                    this.addEnergy(entity, 10.0D);
                }
                if (this.cooldown > 0) {
                    --this.cooldown;
                } else {
                    if (this.cooldown <= 0 && this.energy <= 0.0D) {
                        this.addEnergy(entity, 1.0D);
                    }
                    if (this.energy <= 0.0D) {
                        this.cooldown = MAX_COOLDOWN;
                    } else {
                        this.addEnergy(entity, 0.05D);
                    }
                }
            }
        }
    }

    public float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource source, float damage) {
        if (super.isPaused()) return damage;
        if (this.invulnerableTime > 0) return 0.0F;

        boolean hasShadow = EntityStatsCapability.get(entity).hasShadow();
        if ((source instanceof ModDamageSource && ((ModDamageSource) source).isBypassingLogia()) || (source.isMagic() && !hasShadow)) {
            return damage;
        }
        if (DevilFruitCapability.get(entity).hasYamiPower()) return damage;

        boolean isImmune = true;
        if (source.getDirectEntity() != null) {
            LivingEntity sourceOwner = null;
            Entity directEntity = source.getDirectEntity();
            if (directEntity instanceof LivingEntity) {
                sourceOwner = (LivingEntity) directEntity;
            } else if (directEntity instanceof AbilityProjectileEntity) {
                sourceOwner = ((AbilityProjectileEntity) directEntity).getThrower();
            } else if (directEntity instanceof ProjectileEntity) {
                Entity projectileOwner = ((ProjectileEntity) directEntity).getOwner();
                if (projectileOwner instanceof LivingEntity) {
                    sourceOwner = (LivingEntity) projectileOwner;
                    isImmune &= !HakiHelper.hasImbuingActive((LivingEntity) projectileOwner);
                }
            }

            if (sourceOwner != null) {
                boolean hasImbuingActive = HakiHelper.hasImbuingActive(sourceOwner);
                boolean hasHardeningActive = HakiHelper.hasHardeningActive(sourceOwner);
                if (source instanceof ModDamageSource) {
                    SourceHakiNature nature = ((ModDamageSource) source).getHakiNature();
                    if (nature == SourceHakiNature.IMBUING && hasImbuingActive) isImmune = false;
                    else if (nature == SourceHakiNature.HARDENING && hasHardeningActive) isImmune = false;
                    else if (nature == SourceHakiNature.SPECIAL && (hasImbuingActive || hasHardeningActive)) isImmune = false;
                } else if (source instanceof EntityDamageSource) {
                    isImmune &= !hasHardeningActive;
                } else if (source instanceof IndirectEntityDamageSource) {
                    isImmune &= !hasImbuingActive;
                }
            }
        }

        if (isImmune && this.energy - damage >= 0.0D) {
            WyHelper.spawnParticleEffect((ParticleEffect) ModParticleEffects.FLAMES_OF_REGEN.get(), entity, entity.getX(), entity.getY(), entity.getZ());
            this.addEnergy(entity, -damage);
            this.invulnerableTime = 10;
            return 0.0F;
        }
        this.cooldown = MAX_COOLDOWN;
        return damage;
    }

    private boolean onConsumption(LivingEntity entity, IAbility ability, int nutrition, float saturationModifier) {
        this.addEnergy(entity, nutrition * saturationModifier * 2.0D);
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    private void renderGauge(PlayerEntity player, MatrixStack matrixStack, int posX, int posY, FlamesOfRegenerationRework ability) {
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().bind(ModResources.WIDGETS);
        RendererHelper.drawAbilityIcon(PhoenixFlyPointRework.INSTANCE, matrixStack, posX, posY - 38, 0, 32.0F, 32.0F);
        String energy = new DecimalFormat("#0.0").format(ability.energy);
        WyHelper.drawStringWithBorder(mc.font, matrixStack, energy, posX + 15 - mc.font.width(energy) / 2, posY - 25, Color.WHITE.getRGB());
    }

    public void addEnergy(LivingEntity entity, double energy) {
        this.energy = MathHelper.clamp(this.energy + energy, 0.0D, MAX_ENERGY);
        if (entity instanceof PlayerEntity) {
            WyNetwork.sendTo(new SUpdatePassiveAbilityDataPacket(entity, this), (PlayerEntity) entity);
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        nbt = super.save(nbt);
        nbt.putDouble("energy", this.energy);
        return nbt;
    }

    @Override
    public void load(CompoundNBT nbt) {
        super.load(nbt);
        this.energy = nbt.getDouble("energy");
    }
}
