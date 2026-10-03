package net.kazi.kazimod.mahoraga;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;

public final class MahoragaDamage {
    public final String attack, category;
    private MahoragaDamage(String attack, String category) { this.attack=attack; this.category=category; }
    public static MahoragaDamage identify(DamageSource source) {
        // Special scripted damage and void damage remain unadaptable, regardless of prior learning.
        if (source == DamageSource.OUT_OF_WORLD || source == ModDamageSource.SPECIAL
                || "special".equalsIgnoreCase(source.msgId)) return new MahoragaDamage("", "");
        String attack = "source:" + source.msgId;
        Entity direct = source.getDirectEntity(), attacker = source.getEntity();
        if (source instanceof AbilityDamageSource && ((AbilityDamageSource)source).getAbilitySource() != null) {
            attack = "ability:" + ((AbilityDamageSource)source).getAbilitySource().getKey() + ":" + source.msgId;
        } else if (direct instanceof ProjectileEntity) {
            attack += ":projectile:" + ForgeRegistries.ENTITIES.getKey(direct.getType());
        } else if (attacker instanceof LivingEntity) {
            ResourceLocation weapon = ForgeRegistries.ITEMS.getKey(((LivingEntity)attacker).getMainHandItem().getItem());
            attack += ":attacker:" + ForgeRegistries.ENTITIES.getKey(attacker.getType()) + ":weapon:" + weapon;
        }
        String category = "";
        if (source instanceof ModDamageSource) {
            ModDamageSource mod = (ModDamageSource)source;
            SourceElement element = mod.getElement();
            if (element != null && element != SourceElement.NONE) category = "element:" + element.name();
            else for (SourceType type : new SourceType[]{SourceType.BULLET, SourceType.SLASH, SourceType.FIST,
                    SourceType.BLUNT, SourceType.INTERNAL, SourceType.PROJECTILE, SourceType.PHYSICAL, SourceType.INDIRECT}) {
                if (mod.hasType(type)) { category="type:"+type.name(); break; }
            }
        }
        if (category.isEmpty()) {
            if (source.isFire()) category="element:FIRE";
            else if (source.isExplosion()) category="element:EXPLOSION";
            else if (source.isProjectile()) category="type:PROJECTILE";
            else if (attacker instanceof LivingEntity) category="type:PHYSICAL";
        }
        // Haki nature is deliberately never part of the adaptation key/category.
        return new MahoragaDamage(attack, category);
    }
    private MahoragaDamage() { this("", ""); }
}
