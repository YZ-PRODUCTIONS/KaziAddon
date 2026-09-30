package net.kazi.kazimod.abilities.VampAwaken;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.entity.EntityType;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityType;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.PassiveAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GaugeComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.api.helpers.RendererHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModResources;
import xyz.pixelatedw.mineminenomi.packets.server.ability.SSyncAbilityPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

@Mod.EventBusSubscriber(modid = "kazimod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SoulsPassive extends PassiveAbility2 {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "souls_passive",
            new Pair[]{ImmutablePair.of(
                    "Holds up to 5 soul of the fallen. Each stack prevents one fatal hit and grants 5 seconds of Kamie-like protection.",
                    null)});

    public static final int MAX_SOUL_STACKS = 5;
    private static final float PROTECTION_TIME = 100.0F;
    private static final int SMOKE_DURATION_TICKS = 40;

    public static final AbilityCore<SoulsPassive> INSTANCE;

    private int soulStacks = MAX_SOUL_STACKS;
    private float protectionTimer = 0.0F;
    private int smokeTicks = 0;

    private final DamageTakenComponent damageTakenComponent;

    public SoulsPassive(AbilityCore<SoulsPassive> core) {
        super(core);
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);

        if (super.isClientSide()) {
            GaugeComponent gaugeComponent = new GaugeComponent(this, this::renderGauge);
            super.addComponents(new AbilityComponent[]{this.damageTakenComponent, gaugeComponent});
        } else {
            super.addComponents(new AbilityComponent[]{this.damageTakenComponent});
        }
    }

    @Override
    public void tick(LivingEntity entity) {
        super.tick(entity);
        if (entity.level.isClientSide) {
            return;
        }

        if (this.protectionTimer > 0.0F) {
            this.protectionTimer--;
        }

        if (this.smokeTicks > 0) {
            this.smokeTicks--;
            this.spawnActivationParticles(entity);
        }
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource source, float damage) {
        if (!entity.level.isClientSide && this.protectionTimer > 0.0F) {
            return 0.0F;
        }

        if (!entity.level.isClientSide && this.shouldPreventFatalDamage(entity, damage) && this.consumeSoulStack(entity)) {
            return 0.0F;
        }

        return damage;
    }

    private boolean shouldPreventFatalDamage(LivingEntity entity, float damage) {
        return this.soulStacks > 0 && damage >= entity.getHealth();
    }

    public boolean consumeSoulStack(LivingEntity entity) {
        if (this.soulStacks <= 0) {
            return false;
        }

        this.soulStacks--;
        this.protectionTimer = PROTECTION_TIME;
        this.smokeTicks = SMOKE_DURATION_TICKS;
        this.spawnActivationBats(entity);
        this.spawnActivationParticles(entity);
        sync(entity);

        if (entity instanceof PlayerEntity) {
            ((PlayerEntity) entity).displayClientMessage(
                    new StringTextComponent(TextFormatting.DARK_RED + "A soul has been consumed. "
                            + this.soulStacks + "/" + MAX_SOUL_STACKS + " remaining. "),
                    true
            );
        }

        return true;
    }

    public int getSoulStacks() {
        return this.soulStacks;
    }

    public boolean addSoulStack(LivingEntity entity) {
        if (this.soulStacks >= MAX_SOUL_STACKS) {
            return false;
        }

        this.soulStacks = MathHelper.clamp(this.soulStacks + 1, 0, MAX_SOUL_STACKS);
        sync(entity);

        if (entity instanceof PlayerEntity) {
            ((PlayerEntity) entity).displayClientMessage(
                    new StringTextComponent(TextFormatting.DARK_RED + "A fallen soul has been reclaimed. "
                            + this.soulStacks + "/" + MAX_SOUL_STACKS + " remaining. "),
                    true
            );
        }

        return true;
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof PlayerEntity)) {
            return;
        }

        LivingEntity killer = event.getSource().getEntity() instanceof LivingEntity
                ? (LivingEntity) event.getSource().getEntity()
                : null;
        if (!(killer instanceof PlayerEntity) || killer == event.getEntityLiving()) {
            return;
        }

        IAbilityData data = AbilityDataCapability.get(killer);
        if (data == null) {
            return;
        }

        SoulsPassive soulsPassive = (SoulsPassive) data.getPassiveAbility(INSTANCE);
        if (soulsPassive != null) {
            soulsPassive.addSoulStack(killer);
        }
    }

    private String getSoulStackDisplay() {
        switch (this.soulStacks) {
            case 5:
                return "V";
            case 4:
                return "IV";
            case 3:
                return "III";
            case 2:
                return "II";
            case 1:
                return "I";
            default:
                return "";
        }
    }

    private void spawnActivationParticles(LivingEntity entity) {
        if (!(entity.level instanceof ServerWorld)) {
            return;
        }

        ServerWorld serverWorld = (ServerWorld) entity.level;
        serverWorld.sendParticles(
                ParticleTypes.SMOKE,
                entity.getX(),
                entity.getY() + entity.getBbHeight() * 0.5D,
                entity.getZ(),
                26,
                0.12D,
                0.12D,
                0.12D,
                0.22D
        );
    }

    private void spawnActivationBats(LivingEntity entity) {
        if (!(entity.level instanceof ServerWorld)) {
            return;
        }

        ServerWorld serverWorld = (ServerWorld) entity.level;
        for (int i = 0; i < 5; i++) {
            BatEntity bat = EntityType.BAT.create(serverWorld);
            if (bat == null) {
                continue;
            }

            double angle = entity.getRandom().nextDouble() * Math.PI * 2.0D;
            double radius = 0.8D + entity.getRandom().nextDouble() * 1.2D;
            double x = entity.getX() + Math.cos(angle) * radius;
            double y = entity.getY() + 0.5D + entity.getRandom().nextDouble() * entity.getBbHeight();
            double z = entity.getZ() + Math.sin(angle) * radius;
            bat.moveTo(x, y, z, entity.yRot, 0.0F);
            bat.setDeltaMovement(
                    (entity.getRandom().nextDouble() - 0.5D) * 0.4D,
                    0.1D + entity.getRandom().nextDouble() * 0.2D,
                    (entity.getRandom().nextDouble() - 0.5D) * 0.4D
            );
            serverWorld.addFreshEntity(bat);
        }
    }

    private void sync(LivingEntity entity) {
        if (entity instanceof PlayerEntity) {
            WyNetwork.sendTo(new SSyncAbilityPacket(entity.getId(), this), (PlayerEntity) entity);
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        nbt.putInt("soulStacks", this.soulStacks);
        nbt.putFloat("protectionTimer", this.protectionTimer);
        nbt.putInt("smokeTicks", this.smokeTicks);
        return nbt;
    }

    @Override
    public void load(CompoundNBT nbt) {
        this.soulStacks = nbt.contains("soulStacks")
                ? MathHelper.clamp(nbt.getInt("soulStacks"), 0, MAX_SOUL_STACKS)
                : MAX_SOUL_STACKS;
        this.protectionTimer = nbt.contains("protectionTimer")
                ? Math.max(0.0F, nbt.getFloat("protectionTimer"))
                : 0.0F;
        this.smokeTicks = nbt.contains("smokeTicks")
                ? Math.max(0, nbt.getInt("smokeTicks"))
                : 0;
    }

    @OnlyIn(Dist.CLIENT)
    public void renderGauge(PlayerEntity player, MatrixStack matrixStack, int posX, int posY, SoulsPassive ability) {
        RenderSystem.enableBlend();
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().bind(ModResources.WIDGETS);
        RendererHelper.drawAbilityIcon(INSTANCE, matrixStack, (float) posX, (float) (posY - 38), 0, 32.0F, 32.0F);

        String label = ability.getSoulStackDisplay();
        WyHelper.drawStringWithBorder(
                mc.font, matrixStack,
                label,
                posX + 16 - mc.font.width(label) / 2,
                posY - 27,
                new Color(110, 0, 0).getRGB()
        );
        RenderSystem.disableBlend();
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>(
                "Souls Passive",
                AbilityCategory.DEVIL_FRUITS,
                AbilityType.PASSIVE,
                SoulsPassive::new
        ))
                .addDescriptionLine(DESCRIPTION)
                .setUnlockCheck(entity -> DevilFruitCapability.get(entity).hasAwakenedFruit())
                .build();
    }
}
