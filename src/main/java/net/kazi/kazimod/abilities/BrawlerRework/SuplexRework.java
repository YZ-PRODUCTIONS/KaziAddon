package net.kazi.kazimod.abilities.BrawlerRework;

import net.MrMagicalCart.cartaddon.abilities.cyborgextra.CartSouthlandSuplexAbility;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;

/**
 * Brawler-facing copy of Cart's Cyborg Southland Suplex implementation.
 * Extending the upstream ability keeps its grab, collision, damage, animation,
 * and cleanup behavior identical while this core retains the Brawler identity.
 */
public class SuplexRework extends CartSouthlandSuplexAbility {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "mineminenomi",
            "suplex",
            new Pair[]{ImmutablePair.of(
                    "Grabs an opponent from the back and launches it into the ground",
                    null)});

    private static final float COOLDOWN = 160.0F;
    private static final float CHARGE_TIME = 20.0F;
    private static final float DAMAGE = 30.0F;

    public static final AbilityCore<SuplexRework> INSTANCE;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public SuplexRework(AbilityCore<SuplexRework> core) {
        super((AbilityCore) core);
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        }
        PlayerEntity player = (PlayerEntity) entity;
        return EntityStatsCapability.get(player).isBrawler()
                && QuestDataCapability.get(player).hasFinishedQuest(CartQuests.BRAWLER_TRIAL_01);
    }

    static {
        INSTANCE = new AbilityCore.Builder<SuplexRework>(
                "Suplex",
                AbilityCategory.STYLE,
                SuplexRework::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        DealDamageComponent.getTooltip(DAMAGE))
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceType(SourceType.FIST)
                .setUnlockCheck(SuplexRework::canUnlock)
                .build();
    }
}
