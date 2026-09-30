//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.KirinRework;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.kazi.kazimod.api.helpers.KaziAbilityHelper;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RequireMorphComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class SlumberFieldAbility extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "slumber_field", new Pair[]{ImmutablePair.of("Creates a drowsy field that fills the area with a sleep-inducing aura, making everyone inside progressively more tired.", (Object)null)});
    private static final ITextComponent NAME = new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.slumber_field", "Slumber Field"));
    private static final ResourceLocation ICON = new ResourceLocation("kazimod", "textures/abilities/slumber_field.png");
    private static final int COOLDOWN = 600;
    private static final int HOLD_TIME = 240;
    private static final int RANGE = 10;
    private static final int TIRED_STACKS_PER_HIT = 2;
    private static final int TIRED_APPLICATION_INTERVAL = 80; // 4 seconds (20 ticks per second * 4)

    public static final AbilityCore<SlumberFieldAbility> INSTANCE;

    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this))
            .addTickEvent(this::duringContinuityEvent)
            .addEndEvent(this::endContinuityEvent);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final RequireMorphComponent requireMorphComponent;

    // Track last tired application time for each entity
    private final Map<UUID, Integer> entityLastHitTick = new HashMap<>();

    public SlumberFieldAbility(AbilityCore<SlumberFieldAbility> core) {
        super(core);
        this.requireMorphComponent = new RequireMorphComponent(this, (MorphInfo)CartMorphs.KIRIN_HEAVY.get(), new MorphInfo[]{(MorphInfo)CartMorphs.KIRIN_FLY.get(), (MorphInfo)CartMorphs.KIRIN_HEAVY_ALT.get()});
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.rangeComponent, this.requireMorphComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.entityLastHitTick.clear(); // Reset tracking when ability is activated
        this.continuousComponent.triggerContinuity(entity, (float)HOLD_TIME);
    }

    private void duringContinuityEvent(LivingEntity player, IAbility ability) {
        int currentTick = (int) this.continuousComponent.getContinueTime();

        for(LivingEntity target : this.rangeComponent.getTargetsInArea(player, (float)RANGE)) {
            UUID targetId = target.getUUID();

            // Apply weakened movement effect continuously (refresh every tick)
            net.kazi.kazimod.events.KirinSleepRecovery.applyMovementPenalty(target, 25);

            // Check if enough time has passed since last application for this entity
            if (!entityLastHitTick.containsKey(targetId)) {
                // First hit - apply immediately
                KaziAbilityHelper.addTiredStacks(target, player, TIRED_STACKS_PER_HIT);
                entityLastHitTick.put(targetId, currentTick);
            } else {
                int lastHitTick = entityLastHitTick.get(targetId);
                int ticksSinceLastHit = currentTick - lastHitTick;

                // Apply tired stacks every 4 seconds (80 ticks)
                if (ticksSinceLastHit >= TIRED_APPLICATION_INTERVAL) {
                    KaziAbilityHelper.addTiredStacks(target, player, TIRED_STACKS_PER_HIT);
                    entityLastHitTick.put(targetId, currentTick);
                }
            }
        }

        // Add Kirin puff particle effects throughout the AOE every 5 ticks for better coverage
        if (currentTick % 5 == 0) {
            // Spawn multiple particles distributed throughout the 10-block AOE
            for (int i = 0; i < 12; i++) {
                // Random angle for circular distribution
                double angle = Math.random() * Math.PI * 2;
                // Random distance within the 10-block range
                double distance = Math.random() * RANGE;
                // Calculate X and Z offsets
                double offsetX = Math.cos(angle) * distance;
                double offsetZ = Math.sin(angle) * distance;
                // Random height variation (0-10 blocks above player)
                double offsetY = Math.random() * 10;

                WyHelper.spawnParticleEffect(
                        (ParticleEffect)CartParticleEffects.KIRIN_PUFF.get(),
                        player,
                        player.getX() + offsetX,
                        player.getY() + offsetY,
                        player.getZ() + offsetZ
                );
            }
        }
    }

    private void endContinuityEvent(LivingEntity player, IAbility ability) {
        this.entityLastHitTick.clear(); // Clear tracking when ability ends
        this.cooldownComponent.startCooldown(player, (float)COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Slumber Field", AbilityCategory.DEVIL_FRUITS, SlumberFieldAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip((float)COOLDOWN),
                        ContinuousComponent.getTooltip((float)HOLD_TIME),
                        RangeComponent.getTooltip((float)RANGE, RangeType.AOE)
                })
                .setSourceElement(SourceElement.POISON) // Changed to POISON (same as Doku Gumo)
                .build();
    }
}
