package net.kazi.kazimod.mahoraga;

import java.util.UUID;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import xyz.pixelatedw.mineminenomi.abilities.haki.HaoshokuHakiInfusionAbility;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.data.entity.haki.HakiDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;

/** Permanent combat effects, without starting abilities that emit Haki overlays or particles. */
final class MahoragaHaki {
    private static final UUID ARMOR=UUID.fromString("07ca2840-d23d-4d30-9f57-d779d9fdc741");
    private static final UUID TOUGHNESS=UUID.fromString("07ca2840-d23d-4d30-9f57-d779d9fdc742");
    private static final UUID RESISTANCE=UUID.fromString("07ca2840-d23d-4d30-9f57-d779d9fdc743");

    static void initialize(MahoragaEntity entity) {
        equip(entity,EquipmentSlotType.HEAD,Items.NETHERITE_HELMET);
        equip(entity,EquipmentSlotType.CHEST,Items.NETHERITE_CHESTPLATE);
        equip(entity,EquipmentSlotType.LEGS,Items.NETHERITE_LEGGINGS);
        equip(entity,EquipmentSlotType.FEET,Items.NETHERITE_BOOTS);
        float exp=CommonConfig.INSTANCE.getHakiExpLimit();
        HakiDataCapability.get(entity).setBusoshokuHakiExp(exp);
        // Match the native full-body Haki attribute bonuses at maximum mastery.
        modifier(entity.getAttribute(Attributes.ARMOR),ARMOR,exp/12.5);
        modifier(entity.getAttribute(Attributes.ARMOR_TOUGHNESS),TOUGHNESS,exp/50.0);
        modifier(entity.getAttribute(ModAttributes.TOUGHNESS.get()),RESISTANCE,8);
    }

    private static void equip(MahoragaEntity entity,EquipmentSlotType slot,Item item) {
        ItemStack stack=new ItemStack(item);
        stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION,4);
        stack.enchant(Enchantments.VANISHING_CURSE,1);
        stack.getOrCreateTag().putBoolean("Unbreakable",true);
        entity.setItemSlot(slot,stack);
        entity.setDropChance(slot,0);
    }

    private static void modifier(ModifiableAttributeInstance attribute,UUID id,double amount) {
        if(attribute==null)return;
        attribute.removeModifier(id);
        attribute.addTransientModifier(new AttributeModifier(id,"Mahoraga invisible full-body Haki",amount,AttributeModifier.Operation.ADDITION));
    }

    static float infuse(MahoragaEntity entity,ModDamageSource source,float damage) {
        source.bypassLogia().setInternal().setHakiNature(SourceHakiNature.SPECIAL);
        return damage+(float)HaoshokuHakiInfusionAbility.getDamageBoost(entity,damage);
    }

    private MahoragaHaki() {}
}
