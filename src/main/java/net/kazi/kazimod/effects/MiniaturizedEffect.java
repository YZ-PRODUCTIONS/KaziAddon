package net.kazi.kazimod.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.ai.attributes.AttributeModifierManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectType;
import net.kazi.kazimod.init.KaziAttributes;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.kazi.kazimod.network.SyncEntitySizePacket;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import xyz.pixelatedw.mineminenomi.api.effects.ModEffect;

import java.util.UUID;

public class MiniaturizedEffect extends ModEffect {

    public static final double SCALE = -0.5;
    public static final UUID MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    public MiniaturizedEffect() {
        super(EffectType.HARMFUL, 0xF4A7C3);
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeModifierManager map, int amp) {
        if (map.hasAttribute(KaziAttributes.SIZE.get())) {
            map.getInstance(KaziAttributes.SIZE.get())
                    .addTransientModifier(new AttributeModifier(
                            MODIFIER_UUID,
                            "Miniaturized size reduction",
                            SCALE,
                            Operation.MULTIPLY_TOTAL
                    ));
        }
        super.addAttributeModifiers(entity, map, amp);
        entity.refreshDimensions();
        KaziPacketHandler.syncEntitySize(entity);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeModifierManager map, int amp) {
        if (map.hasAttribute(KaziAttributes.SIZE.get())) {
            map.getInstance(KaziAttributes.SIZE.get())
                    .removeModifier(MODIFIER_UUID);
        }
        super.removeAttributeModifiers(entity, map, amp);
        entity.refreshDimensions();
        KaziPacketHandler.syncEntitySize(entity);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (!entity.hasEffect(net.kazi.kazimod.init.KaziEffects.MINIATURIZED.get())) return;
        event.setAmount(event.getAmount() * 1.5F);
    }
}