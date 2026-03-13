//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.KaruRework;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeMod;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.karu.KarmaAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.MorphAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModI18n;
import xyz.pixelatedw.mineminenomi.init.ModMorphs;

public class IngaZarashiRework extends MorphAbility2 {
    private static final ITextComponent[] DESCRIPTION;
    private static final int MIN_COOLDOWN = 20;
    private static final int MAX_COOLDOWN = 240;
    public static final AbilityCore<IngaZarashiRework> INSTANCE;
    private static final UUID ARMOR_MODIFIER_UUID;
    private static final UUID ATTACK_MODIFIER_UUID;
    private static final UUID REACH_MODIFIER_UUID;
    private Optional<KarmaAbility> karmaAbility = Optional.empty();
    private final DamageTakenComponent damageTakenComponent;

    public IngaZarashiRework(AbilityCore<IngaZarashiRework> core) {
        super(core);
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);
        super.addComponents(new AbilityComponent[]{this.damageTakenComponent});
        this.addCanUseCheck(this::canUse);
        this.continuousComponent.addTickEvent(this::duringContinuousEvent);
        this.continuousComponent.addEndEvent(this::endContinuousEvent);
    }

    private void duringContinuousEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.getContinueTime() % 20.0F == 0.0F && this.karmaAbility.isPresent()) {
            this.updateKarma(entity);
        }

    }

    private void endContinuousEvent(LivingEntity entity, IAbility ability) {
        float cooldown = MathHelper.clamp(this.continuousComponent.getContinueTime(), 20.0F, 240.0F);
        this.cooldownComponent.startCooldown(entity, cooldown);
    }

    private AbilityUseResult canUse(LivingEntity entity, IAbility ability) {
        IAbilityData props = AbilityDataCapability.get(entity);
        KarmaAbility karma = (KarmaAbility)props.getPassiveAbility(KarmaAbility.INSTANCE);
        if (karma == null) {
            return AbilityUseResult.fail((ITextComponent)null);
        } else {
            this.karmaAbility = Optional.ofNullable(karma);
            return AbilityUseResult.success();
        }
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
        if (AbilityHelper.isDodging(entity)) {
            return damage;
        } else if (this.continuousComponent.isContinuous() && this.karmaAbility.isPresent()) {
            // Calculate damage reduction based on karma
            // At 100 karma = 75% reduction (0.25 multiplier)
            // At 50 karma = 37.5% reduction (0.625 multiplier)
            float karma = ((KarmaAbility)this.karmaAbility.get()).getKarma();
            float damageReduction = Math.min(karma / 100.0F * 0.75F, 0.75F); // Max 75% reduction at 100 karma
            float damageMultiplier = 1.0F - damageReduction;

            return damage * damageMultiplier;
        } else {
            return damage;
        }
    }

    private void updateKarma(LivingEntity entity) {
        if (((KarmaAbility)this.karmaAbility.get()).getPrevKarma() != ((KarmaAbility)this.karmaAbility.get()).getKarma()) {
            this.statsComponent.removeModifiers(entity);

            for(Map.Entry<Attribute, AttributeModifier> entry : this.getAttributes().entries()) {
                this.statsComponent.removeAttributeModifier((Attribute)entry.getKey());
                this.statsComponent.addAttributeModifier((Attribute)entry.getKey(), (AttributeModifier)entry.getValue());
            }

            this.statsComponent.applyModifiers(entity);
            this.morphComponent.updateMorphSize(entity);
            ((KarmaAbility)this.karmaAbility.get()).setPrevKarma(((KarmaAbility)this.karmaAbility.get()).getKarma());
        }

        ((KarmaAbility)this.karmaAbility.get()).addKarma(entity, -(((KarmaAbility)this.karmaAbility.get()).getKarma() / 100.0F));
    }

    private Multimap<Attribute, AttributeModifier> getAttributes() {
        Multimap<Attribute, AttributeModifier> map = HashMultimap.create();
        float karma = ((KarmaAbility)this.karmaAbility.get()).getKarma();
        double armorMod = (double)Math.min(karma / 100.0F * 8.0F, 8.0F);
        double attackMod = (double)Math.min(karma / 100.0F * 10.0F, 10.0F);
        double reachMod = Math.min((double)(karma / 100.0F) * (double)2.5F, (double)2.5F);
        map.put(Attributes.ARMOR, new AbilityAttributeModifier(ARMOR_MODIFIER_UUID, INSTANCE, "Karma Armor Modifier", armorMod, Operation.ADDITION));
        map.put(ModAttributes.PUNCH_DAMAGE.get(), new AbilityAttributeModifier(ATTACK_MODIFIER_UUID, INSTANCE, "Karma Attack Modifier", attackMod, Operation.ADDITION));
        AbilityAttributeModifier reachAttribute = new AbilityAttributeModifier(REACH_MODIFIER_UUID, INSTANCE, "Karma Reach Modifier", reachMod, Operation.ADDITION);
        map.put(ForgeMod.REACH_DISTANCE.get(), reachAttribute);
        map.put(ModAttributes.ATTACK_RANGE.get(), reachAttribute);
        return map;
    }

    public MorphInfo getTransformation() {
        return (MorphInfo)ModMorphs.INGA_ZARASHI.get();
    }

    static {
        DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "inga_zarashi", new Pair[]{ImmutablePair.of("Increases your physical prowess depending on how much damage you have in your Karma counter", new Object[]{AbilityHelper.mentionText(ModI18n.GUI_KARMA)})});
        INSTANCE = (new AbilityCore.Builder("Inga Zarashi", AbilityCategory.DEVIL_FRUITS, IngaZarashiRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(20.0F, 240.0F), ContinuousComponent.getTooltip()}).build();
        ARMOR_MODIFIER_UUID = UUID.fromString("06141405-6e5c-4b98-a8f7-230e0ffb96bc");
        ATTACK_MODIFIER_UUID = UUID.fromString("7ddb710f-a497-4f64-b272-8fcc9955b401");
        REACH_MODIFIER_UUID = UUID.fromString("dc0d06d6-ffd6-49d8-b484-da232b78fd41");
    }
}