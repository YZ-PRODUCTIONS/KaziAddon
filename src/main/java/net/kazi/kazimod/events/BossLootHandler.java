package net.kazi.kazimod.events;

import java.util.Collection;
import net.kazi.kazimod.hermano.HermanoEntity;
import net.kazi.kazimod.init.KaziItems2;
import net.kazi.kazimod.jerome.JeromeEntity;
import net.kazi.kazimod.longbob.LongbobEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.GameRules;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.init.ModItems;

@Mod.EventBusSubscriber(modid = "kazimod")
public final class BossLootHandler {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity boss = event.getEntityLiving();
        if (boss.level.isClientSide || !isWorldBoss(boss)
                || !boss.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;

        Item essence = KaziItems2.AWAKENING_ESSENCE.get();
        Item gold = ModItems.TIER_3_BOX.get();
        Item iron = ModItems.TIER_2_BOX.get();
        Item wood = ModItems.TIER_1_BOX.get();
        int goldCount = 1 + (boss.getRandom().nextFloat() < .25F ? 1 : 0);
        int ironCount = 1 + (boss.getRandom().nextFloat() < .50F ? 1 : 0);
        int woodCount = 1 + (boss.getRandom().nextFloat() < .75F ? 1 : 0);

        Collection<ItemEntity> drops = event.getDrops();
        // Replace these rewards only, including the world bosses' old essence drop.
        drops.removeIf(drop -> {
            Item item = drop.getItem().getItem();
            return item == essence || item == gold || item == iron || item == wood;
        });
        addDrop(drops, boss, essence, 1);
        addDrop(drops, boss, gold, goldCount);
        addDrop(drops, boss, iron, ironCount);
        addDrop(drops, boss, wood, woodCount);
    }

    private static boolean isWorldBoss(LivingEntity entity) {
        return entity instanceof HermanoEntity || entity instanceof LongbobEntity || entity instanceof JeromeEntity;
    }

    private static void addDrop(Collection<ItemEntity> drops, LivingEntity boss, Item item, int count) {
        ItemEntity drop = new ItemEntity(boss.level, boss.getX(), boss.getY() + .5, boss.getZ(), new ItemStack(item, count));
        drop.setDefaultPickUpDelay();
        drops.add(drop);
    }

    private BossLootHandler() {}
}
