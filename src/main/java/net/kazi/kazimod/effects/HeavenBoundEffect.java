package net.kazi.kazimod.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.potion.EffectType;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.kazi.kazimod.init.KaziEffects;
import xyz.pixelatedw.mineminenomi.api.effects.ModEffect;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;

/** Refreshed briefly by an existing chain cast; never leaves a saved permanent restraint. */
@Mod.EventBusSubscriber(modid = "kazimod")
public final class HeavenBoundEffect extends ModEffect {
    public HeavenBoundEffect() {
        super(EffectType.HARMFUL, 0xEAC65B);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "f4cfe3dd-7234-43d4-a117-cb5077bfb911", -1, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(ForgeMod.SWIM_SPEED.get(), "d64c285c-5158-40bd-9081-19b958eaeae6", -1, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(ModAttributes.JUMP_HEIGHT.get(), "b971bd1f-b907-4f8f-847c-a54a03d183a5", -1, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, "c9d38a0b-49f8-4dba-bc35-5ad1b2380aa6", 1, AttributeModifier.Operation.ADDITION);
    }
    @Override public boolean isDurationEffectTick(int duration, int amplifier) { return true; }
    @Override public void applyEffectTick(LivingEntity entity, int amplifier) { stopMovement(entity); }
    @Override public boolean isBlockingSwings() { return true; }

    private static void stopMovement(LivingEntity entity) {
        entity.setJumping(false);
        Vector3d movement = entity.getDeltaMovement();
        // The cast supplies a short Float refresh separately; the lock itself never changes gravity.
        entity.setDeltaMovement(0, Math.min(0, movement.y), 0);
    }
    @SubscribeEvent public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntityLiving().hasEffect(KaziEffects.HEAVEN_BOUND.get())) stopMovement(event.getEntityLiving());
    }
}
