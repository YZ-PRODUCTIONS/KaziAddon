package net.kazi.kazimod.abilities.swordsmanrework;

import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import net.minecraft.world.server.ServerWorld;
import java.util.Random;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModQuests;

public class FoxfireStyleAbility extends Ability {

    // No HOLD_TIME — the stance lasts until a foxfire ability is used, like K-Room
    private static final float COOLDOWN = 200.0F;

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "foxfire_style",
            new Pair[]{
                    ImmutablePair.of(
                            "The user ignites their blade with foxfire flames, enabling fire-imbued sword techniques. " +
                                    "The stance ends after using a foxfire-imbued ability.",
                            new Object[]{}
                    )
            }
    );

    public static final AbilityCore<FoxfireStyleAbility> INSTANCE;

    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this))
            .addStartEvent(100, this::startContinuityEvent)
            .addTickEvent(100, this::tickContinuityEvent)
            .addEndEvent(100, this::endContinuityEvent);

    private Interval particleInterval;
    private static final Random RANDOM = new Random();

    // K-Room pattern: set to true by child abilities after they fire in foxfire mode
    private boolean abilityUsed = false;

    public FoxfireStyleAbility(AbilityCore<FoxfireStyleAbility> core) {
        super(core);
        this.particleInterval = new Interval(3);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.continuousComponent
        });
        this.addUseEvent(this::useEvent);
        this.addCanUseCheck(AbilityHelper::canUseSwordsmanAbilities);
        this.addCanUseCheck(AbilityLimits::fruitless);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (!this.continuousComponent.isContinuous()) {
            // Activate the stance — no hold time cap, runs until ability is used
            this.abilityUsed = false;
            this.continuousComponent.startContinuity(entity);
        } else {
            // Allow manual toggle-off if desired
            this.continuousComponent.stopContinuity(entity);
        }
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.abilityUsed = false;

        HiryuKaenRework hiryuKaen = getEquippedAbility(entity, HiryuKaenRework.INSTANCE, HiryuKaenRework.class);
        if (hiryuKaen != null) hiryuKaen.switchFoxfireMode(entity);

        YakkodoriRework yakkodori = getEquippedAbility(entity, YakkodoriRework.INSTANCE, YakkodoriRework.class);
        if (yakkodori != null) yakkodori.switchFoxfireMode(entity);

        SanbyakurokujoPoundHoRework poundHo = getEquippedAbility(entity, SanbyakurokujoPoundHoRework.INSTANCE, SanbyakurokujoPoundHoRework.class);
        if (poundHo != null) poundHo.switchFoxfireMode(entity);
    }

    private void tickContinuityEvent(LivingEntity entity, IAbility ability) {
        // End the stance as soon as a foxfire ability has been used (K-Room pattern)
        if (this.abilityUsed) {
            this.continuousComponent.stopContinuity(entity);
            return;
        }

        if (!entity.level.isClientSide && this.particleInterval.canTick()) {
            ServerWorld world = (ServerWorld) entity.level;
            double x = entity.getX();
            double y = entity.getY();
            double z = entity.getZ();

            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI / 6) * i + (entity.tickCount * 0.05);
                double radius = 0.6 + RANDOM.nextDouble() * 0.4;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;
                double offsetY = RANDOM.nextDouble() * entity.getBbHeight();
                world.sendParticles(net.minecraft.particles.ParticleTypes.FLAME,
                        x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.0, 0.0, 0.01);
            }

            for (int i = 0; i < 3; i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * 1.2;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * 1.2;
                double offsetY = RANDOM.nextDouble() * entity.getBbHeight();
                world.sendParticles(net.minecraft.particles.ParticleTypes.SOUL_FIRE_FLAME,
                        x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.0, 0.0, 0.01);
            }
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        HiryuKaenRework hiryuKaen = getEquippedAbility(entity, HiryuKaenRework.INSTANCE, HiryuKaenRework.class);
        if (hiryuKaen != null) hiryuKaen.switchNormalMode(entity);

        YakkodoriRework yakkodori = getEquippedAbility(entity, YakkodoriRework.INSTANCE, YakkodoriRework.class);
        if (yakkodori != null) yakkodori.switchNormalMode(entity);

        SanbyakurokujoPoundHoRework poundHo = getEquippedAbility(entity, SanbyakurokujoPoundHoRework.INSTANCE, SanbyakurokujoPoundHoRework.class);
        if (poundHo != null) poundHo.switchNormalMode(entity);

        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    /**
     * Called by HiryuKaenRework, YakkodoriRework, and SanbyakurokujoPoundHoRework
     * after they fire in foxfire mode, mirroring KRoomAnesthesiaAbility.setAbilityUsed().
     * This ends the foxfire stance and starts the cooldown on the next tick.
     */
    public void setAbilityUsed(boolean used) {
        this.abilityUsed = used;
    }

    // =========================================================
    // HELPERS
    // =========================================================

    static <T extends Ability> T getEquippedAbility(LivingEntity entity, AbilityCore<T> core, Class<T> clazz) {
        if (!(entity instanceof PlayerEntity)) return null;
        try {
            IAbilityData props = AbilityDataCapability.get(entity);
            if (props == null) return null;
            return clazz.cast(props.getEquippedAbility(core));
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) return false;
        PlayerEntity player = (PlayerEntity) entity;
        IEntityStats props = EntityStatsCapability.get(player);
        IQuestData questProps = QuestDataCapability.get(player);
        return props.isSwordsman() && questProps.hasFinishedQuest(ModQuests.SWORDSMAN_TRIAL_05);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Foxfire Style", AbilityCategory.STYLE, FoxfireStyleAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        // No ContinuousComponent tooltip — the duration is open-ended
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .setSourceType(new SourceType[]{SourceType.SLASH})
                .setSourceElement(SourceElement.FIRE)
                .setUnlockCheck(FoxfireStyleAbility::canUnlock)
                .build();
    }
}