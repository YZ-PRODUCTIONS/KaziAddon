package net.kazi.kazimod.abilities.Kyoka;

import net.kazi.kazimod.entities.ShadowDoppelmanEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
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
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IllusionCloneBarrageAbility extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod",
            "illusion_clone_barrage",
            new Pair[]{ImmutablePair.of("Creates three illusionary doubles around the user, each copying the user's combat style while only carrying a quarter of their vitality.", null)}
    );
    private static final float HOLD_TIME = 220.0F;
    private static final float COOLDOWN = 420.0F;
    private static final double CLONE_DISTANCE = 5.0D;
    public static final AbilityCore<IllusionCloneBarrageAbility> INSTANCE;

    private final ContinuousComponent continuousComponent = new ContinuousComponent(this, true)
            .addStartEvent(this::startEvent)
            .addTickEvent(this::tickEvent)
            .addEndEvent(this::endEvent);
    private final List<ShadowDoppelmanEntity> clones = new ArrayList<>();

    public IllusionCloneBarrageAbility(AbilityCore<IllusionCloneBarrageAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent});
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else {
            this.continuousComponent.startContinuity(entity, HOLD_TIME);
        }
    }

    private void startEvent(LivingEntity entity, IAbility ability) {
        clearClones();
        entity.level.playSound(null, entity.blockPosition(), ModSounds.KENBUNSHOKU_HAKI_ON_SFX.get(), SoundCategory.PLAYERS, 1.2F, 1.1F);

        for (int i = 0; i < 3; i++) {
            double angle = Math.toRadians(entity.yBodyRot + (i * 120.0D));
            Vector3d spawnPos = new Vector3d(
                    entity.getX() - Math.sin(angle) * CLONE_DISTANCE,
                    entity.getY(),
                    entity.getZ() + Math.cos(angle) * CLONE_DISTANCE
            );
            ShadowDoppelmanEntity clone = KyokaCloneHelper.spawnClone(entity, spawnPos, 0.25F, false);
            if (clone != null) {
                this.clones.add(clone);
            }
        }
    }

    private void tickEvent(LivingEntity entity, IAbility ability) {
        Iterator<ShadowDoppelmanEntity> iterator = this.clones.iterator();
        while (iterator.hasNext()) {
            ShadowDoppelmanEntity clone = iterator.next();
            if (clone == null || !clone.isAlive()) {
                iterator.remove();
            }
        }

        if (this.clones.isEmpty()) {
            this.continuousComponent.stopContinuity(entity);
        }
    }

    private void endEvent(LivingEntity entity, IAbility ability) {
        clearClones();
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    private void clearClones() {
        for (ShadowDoppelmanEntity clone : this.clones) {
            if (clone != null && clone.isAlive()) {
                clone.remove();
            }
        }
        this.clones.clear();
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Illusion Clone Barrage", AbilityCategory.DEVIL_FRUITS, IllusionCloneBarrageAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        ContinuousComponent.getTooltip(HOLD_TIME)
                )
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .build();
    }
}
