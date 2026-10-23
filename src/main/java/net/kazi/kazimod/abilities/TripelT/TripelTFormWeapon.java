package net.kazi.kazimod.abilities.TripelT;

import net.kazi.kazimod.init.KaziItems2;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;

/** Weapons belong to the transformation, not to a separately toggled summon. */
public final class TripelTFormWeapon {
    private static final String TAG = "KaziTripleTFormWeapon";

    public static void registerEvents() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(TripelTFormWeapon::login);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) -> {
            remove(e.getPlayer(), false);
            remove(e.getPlayer(), true);
        });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.living.LivingDeathEvent e) -> {
            remove(e.getEntityLiving(), false);
            remove(e.getEntityLiving(), true);
        });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.item.ItemTossEvent e) -> {
            if (summoned(e.getEntityItem().getItem())) e.setCanceled(true);
        });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.PlayerTickEvent e) -> {
            if (e.phase != net.minecraftforge.event.TickEvent.Phase.END || e.player.level.isClientSide) return;
            if (!net.kazi.kazimod.init.KaziMorphs.TRIPEL_T.get().isActive(e.player)) remove(e.player, false);
            if (!TripelTHelper.isGodForm(e.player)) remove(e.player, true);
        });
    }

    private static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEntity player = event.getPlayer();
        if (player.level.isClientSide) return;
        xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData data = AbilityDataCapability.get(player);
        if (data == null) return;
        boolean changed = false;
        if (data.hasUnlockedAbility(TripelTArmoryAbility.INSTANCE)) {
            xyz.pixelatedw.mineminenomi.api.abilities.IAbility old = data.getEquippedAbility(TripelTArmoryAbility.INSTANCE);
            if (old instanceof TripelTArmoryAbility) ((TripelTArmoryAbility) old).dismiss(player);
            data.removeUnlockedAbility(TripelTArmoryAbility.INSTANCE);
            changed = true;
        }
        if (data.hasUnlockedAbility(JudgementationAbility.INSTANCE)) {
            // Snapshot the slot before removing the unlock, which also clears equipped slots.
            java.util.List<xyz.pixelatedw.mineminenomi.api.abilities.IAbility> equipped = data.getRawEquippedAbilities();
            int replacementSlot = -1;
            boolean counterEquipped = false;
            for (int i = 0; i < equipped.size(); i++) {
                xyz.pixelatedw.mineminenomi.api.abilities.IAbility ability = equipped.get(i);
                if (ability == null) continue;
                if (ability.getCore() == JudgementationAbility.INSTANCE) replacementSlot = i;
                if (ability.getCore() == SwingingCounterAbility.INSTANCE) counterEquipped = true;
            }
            if (!data.hasUnlockedAbility(SwingingCounterAbility.INSTANCE))
                data.addUnlockedAbility(SwingingCounterAbility.INSTANCE, data.getUnlockTypeForAbility(JudgementationAbility.INSTANCE));
            data.removeUnlockedAbility(JudgementationAbility.INSTANCE);
            if (!counterEquipped && replacementSlot >= 0)
                data.setEquippedAbility(replacementSlot, SwingingCounterAbility.INSTANCE.createAbility());
            changed = true;
        }
        if (!changed) return;
        xyz.pixelatedw.mineminenomi.wypi.WyNetwork.sendToAllTrackingAndSelf(
                new xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket(player.getId(), data), player);
    }

    private static boolean summoned(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG)
                && (stack.getItem() == KaziItems2.TRIPLE_T_BAT.get()
                || stack.getItem() == KaziItems2.TRIPLE_T_STAFF.get());
    }

    public static AbilityUseResult canStart(LivingEntity entity, boolean alreadyActive) {
        if (alreadyActive || !(entity instanceof PlayerEntity) || entity.getMainHandItem().isEmpty()
                || summoned(entity.getMainHandItem()) || ((PlayerEntity) entity).inventory.getFreeSlot() >= 0) {
            return AbilityUseResult.success();
        }
        return AbilityUseResult.fail(new StringTextComponent("Free one inventory slot for your held item before transforming."));
    }

    public static void grant(LivingEntity entity, boolean god) {
        if (entity.level.isClientSide) return;
        // Stop a summon left equipped in saves made before weapons were merged into forms.
        xyz.pixelatedw.mineminenomi.api.abilities.IAbility old = AbilityDataCapability.get(entity)
                .getEquippedAbility(TripelTArmoryAbility.INSTANCE);
        if (old instanceof TripelTArmoryAbility) ((TripelTArmoryAbility) old).dismiss(entity);
        remove(entity, false);
        remove(entity, true);
        ItemStack held = entity.getMainHandItem();
        if (!held.isEmpty()) {
            if (!(entity instanceof PlayerEntity)) return;
            PlayerEntity player = (PlayerEntity) entity;
            int slot = player.inventory.getFreeSlot();
            if (slot < 0) return;
            player.inventory.setItem(slot, held);
        }
        ItemStack weapon = new ItemStack(god ? KaziItems2.TRIPLE_T_STAFF.get() : KaziItems2.TRIPLE_T_BAT.get());
        weapon.getOrCreateTag().putString(TAG, god ? "staff" : "bat");
        entity.setItemInHand(Hand.MAIN_HAND, weapon);
    }

    public static void remove(LivingEntity entity, boolean god) {
        if (entity.level.isClientSide) return;
        String kind = god ? "staff" : "bat";
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            for (int i = 0; i < player.inventory.getContainerSize(); i++) {
                ItemStack stack = player.inventory.getItem(i);
                if (summoned(stack) && kind.equals(stack.getTag().getString(TAG))) player.inventory.setItem(i, ItemStack.EMPTY);
            }
        } else for (Hand hand : Hand.values()) {
            ItemStack stack = entity.getItemInHand(hand);
            if (summoned(stack) && kind.equals(stack.getTag().getString(TAG))) entity.setItemInHand(hand, ItemStack.EMPTY);
        }
    }
}
