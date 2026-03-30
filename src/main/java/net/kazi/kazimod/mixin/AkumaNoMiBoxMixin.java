package net.kazi.kazimod.mixin;

import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameterSets;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.LootTable;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.OneFruitEntry;
import xyz.pixelatedw.mineminenomi.api.events.onefruit.InventoryDevilFruitEvent;
import xyz.pixelatedw.mineminenomi.api.helpers.DevilFruitHelper;
import xyz.pixelatedw.mineminenomi.data.world.OFPWWorldData;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiBoxItem;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;

@Mixin(value = AkumaNoMiBoxItem.class, remap = false)
public abstract class AkumaNoMiBoxMixin {

    @Shadow public static Pair<Integer, ResourceLocation> TIER_1_FRUITS;
    @Shadow public static Pair<Integer, ResourceLocation> TIER_2_FRUITS;
    @Shadow public static Pair<Integer, ResourceLocation> TIER_3_FRUITS;
    @Shadow private Pair<Integer, ResourceLocation> tier;

    @Shadow public abstract int getKeySlot(PlayerEntity player);

    @Unique private static final int KAZI_MAX_FRUIT_ROLL_ATTEMPTS = 256;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void overrideBoxTables(CallbackInfo ci) {
        TIER_1_FRUITS = ImmutablePair.of(1, new ResourceLocation("kazimod", "dfboxes/wooden_box"));
        TIER_2_FRUITS = ImmutablePair.of(2, new ResourceLocation("kazimod", "dfboxes/iron_box"));
        TIER_3_FRUITS = ImmutablePair.of(3, new ResourceLocation("kazimod", "dfboxes/golden_box"));
    }

    @Inject(
            method = {
                    "use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;",
                    "func_77659_a(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"
            },
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void kazi$useBox(World world, PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult<ItemStack>> cir) {
        ItemStack boxStack = player.getItemInHand(hand);
        if (world.isClientSide) {
            cir.setReturnValue(ActionResult.success(boxStack));
            return;
        }

        int keySlot = this.getKeySlot(player);
        if (keySlot < 0 || hand == Hand.OFF_HAND) {
            cir.setReturnValue(ActionResult.fail(boxStack));
            return;
        }

        player.inventory.removeItem(keySlot, 1);
        player.inventory.removeItem(boxStack);

        LootTable lootTable = world.getServer().getLootTables().get(this.tier.getValue());
        ItemStack fruitStack = kazi$rollFruit((ServerWorld) world, player, lootTable);
        if (fruitStack.isEmpty()) {
            player.inventory.removeItem(boxStack);
            cir.setReturnValue(ActionResult.success(player.getItemInHand(hand)));
            return;
        }

        if (!(fruitStack.getItem() instanceof AkumaNoMiItem)) {
            player.inventory.add(fruitStack);
            cir.setReturnValue(new ActionResult<>(ActionResultType.SUCCESS, player.getItemInHand(hand)));
            return;
        }

        if (DevilFruitHelper.hasDFLimitInInventory(player)) {
            player.drop(fruitStack, true);
            cir.setReturnValue(new ActionResult<>(ActionResultType.SUCCESS, player.getItemInHand(hand)));
            return;
        }

        AkumaNoMiItem fruitItem = (AkumaNoMiItem) fruitStack.getItem();
        player.inventory.add(fruitStack);

        String source = "Obtained from" + boxStack.getDisplayName().getString();
        OFPWWorldData worldData = OFPWWorldData.get();
        worldData.updateOneFruit(fruitItem.getRegistryName(), player.getUUID(), OneFruitEntry.Status.INVENTORY, source);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new InventoryDevilFruitEvent(player, fruitItem, source));

        cir.setReturnValue(new ActionResult<>(ActionResultType.SUCCESS, player.getItemInHand(hand)));
    }

    @Unique
    private ItemStack kazi$rollFruit(ServerWorld world, PlayerEntity player, LootTable lootTable) {
        for (int attempt = 0; attempt < KAZI_MAX_FRUIT_ROLL_ATTEMPTS; attempt++) {
            LootContext context = new LootContext.Builder(world)
                    .withParameter(LootParameters.THIS_ENTITY, player)
                    .create(LootParameterSets.EMPTY);
            List<ItemStack> loot = lootTable.getRandomItems(context);
            for (ItemStack stack : loot) {
                if (stack != null && !stack.isEmpty()) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
