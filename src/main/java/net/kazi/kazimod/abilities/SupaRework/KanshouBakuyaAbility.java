package net.kazi.kazimod.abilities.SupaRework;

import net.kazi.kazimod.init.KaziItems2;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityType;
import xyz.pixelatedw.mineminenomi.api.abilities.PassiveAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ItemSpawnComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;

/** Supa awakening copy of Ama no Murakumo, bound to the Kanshou & Bakuya weapon. */
public class KanshouBakuyaAbility extends PassiveAbility2 {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "kanshou_bakuya_ability",
            new Pair[]{ImmutablePair.of("Projects Kanshou & Bakuya into the user's hand.", null)});

    public static final AbilityCore<KanshouBakuyaAbility> INSTANCE =
            new AbilityCore.Builder<>(
                    "Kanshou and Bakuya",
                    AbilityCategory.DEVIL_FRUITS,
                    AbilityType.PASSIVE,
                    KanshouBakuyaAbility::new)
                    .addDescriptionLine(DESCRIPTION)
                    .setSourceHakiNature(SourceHakiNature.IMBUING)
                    .setSourceType(SourceType.SLASH)
                    .setUnlockCheck(user -> DevilFruitCapability.get(user).hasAwakenedFruit())
                    .build();

    private final ContinuousComponent continuousComponent = new ContinuousComponent(this, true)
            .addStartEvent(this::onContinuityStart)
            .addEndEvent(this::onContinuityEnd);
    private final ItemSpawnComponent itemSpawnComponent = new ItemSpawnComponent(this);

    public KanshouBakuyaAbility(AbilityCore<KanshouBakuyaAbility> core) {
        super(core);
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.itemSpawnComponent});
        this.addEquipEvent((entity, ability) -> ensureInventoryPair(entity));
    }

    @Override
    public void use(LivingEntity entity) {
        this.continuousComponent.triggerContinuity(entity);
    }

    @Override
    public void tick(LivingEntity entity) {
        ensureInventoryPair(entity);
        super.tick(entity);
    }

    private void onContinuityStart(LivingEntity entity, xyz.pixelatedw.mineminenomi.api.abilities.IAbility ability) {
        this.itemSpawnComponent.spawnItem(entity, new ItemStack((IItemProvider) KaziItems2.KANSHOU_BAKUYA.get()));
    }

    private void onContinuityEnd(LivingEntity entity, xyz.pixelatedw.mineminenomi.api.abilities.IAbility ability) {
        this.itemSpawnComponent.despawnItems(entity);
    }

    private static void ensureInventoryPair(LivingEntity entity) {
        if (entity.level.isClientSide || !(entity instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) entity;
        if (hasInventoryPair(player)) return;
        ItemStack stack = new ItemStack(KaziItems2.KANSHOU_BAKUYA.get());
        if (!player.inventory.add(stack) && !stack.isEmpty()) {
            player.displayClientMessage(new StringTextComponent("Kanshou & Bakuya needs an empty inventory slot."), true);
        }
    }

    private static boolean hasInventoryPair(PlayerEntity player) {
        for (ItemStack stack : player.inventory.items) {
            if (isPair(stack)) return true;
        }
        for (ItemStack stack : player.inventory.offhand) {
            if (isPair(stack)) return true;
        }
        for (ItemStack stack : player.inventory.armor) {
            if (isPair(stack)) return true;
        }
        return false;
    }

    private static boolean isPair(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == KaziItems2.KANSHOU_BAKUYA.get();
    }
}
