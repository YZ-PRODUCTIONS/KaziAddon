package net.kazi.kazimod.abilities.GoruRework;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.MrMagicalCart.cartaddon.init.CartAbilities;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityType;
import xyz.pixelatedw.mineminenomi.api.abilities.PassiveAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GaugeComponent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

/** Health-driven form selector shared by the Goru awakening's three attack slots. */
public final class SeriousnessAbility extends PassiveAbility2 {
    public static final AbilityCore<SeriousnessAbility> INSTANCE = new AbilityCore.Builder<>(
            "Seriousness", AbilityCategory.DEVIL_FRUITS, AbilityType.PASSIVE, SeriousnessAbility::new)
            .setIcon(new ResourceLocation("kazimod", "textures/abilities/rage_rush.png"))
            .addDescriptionLine(
                    new TranslationTextComponent("ability.kazimod.seriousness.description.0"),
                    new TranslationTextComponent("ability.kazimod.seriousness.description.1"),
                    new TranslationTextComponent("ability.kazimod.seriousness.description.2"),
                    new TranslationTextComponent("ability.kazimod.seriousness.description.3"),
                    new TranslationTextComponent("ability.kazimod.seriousness.description.4"),
                    new TranslationTextComponent("ability.kazimod.seriousness.description.5"))
            .addAdvancedDescriptionLine((user, ability) -> new StringTextComponent(
                    "Current health: " + getHealthPercentage(user) + "%"))
            .setUnlockCheck(SeriousnessAbility::isEligible)
            .build();

    private final net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsSequence drawSequence =
            new net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsSequence(.5);
    private net.minecraft.world.World drawWorld;

    public SeriousnessAbility(AbilityCore<SeriousnessAbility> core) {
        super(core);
        addRemoveEvent((user, ability) -> {
            if (!user.level.isClientSide) net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutscenes.cancel(user);
            drawSequence.stop();
        });
        if (super.isClientSide()) {
            this.addComponents(new GaugeComponent(this, this::renderGauge));
        }
    }

    public static boolean canPlayDraw(LivingEntity user) {
        return canContinueDraw(user)
                && user.getHealth() < user.getMaxHealth() * .5F;
    }

    public static boolean canContinueDraw(LivingEntity user) {
        return getActivePassive(user) != null && user.isAlive() && user.getHealth() > 0;
    }

    /** Reset the replay timer without interrupting a scene or rearming its health-cycle latch. */
    public static void resetCutsceneCooldown(LivingEntity user) {
        if (user.level.isClientSide) return;
        net.minecraft.nbt.CompoundNBT data = user.getPersistentData();
        if (user instanceof PlayerEntity) data = data.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        data.remove("kazimod:ea_draw_next_cutscene");
    }

    @Override public void tick(LivingEntity user) {
        if (!user.level.isClientSide) {
            if (drawWorld != null && drawWorld != user.level) {
                net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutscenes.cancel(user);
                drawSequence.stop();
            }
            drawWorld = user.level;
            boolean eligible = canContinueDraw(user);
            boolean start = drawSequence.update(user.getHealth(), user.getMaxHealth(), user.level.getGameTime(), eligible);
            if (!eligible) net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutscenes.cancel(user);
            else if (start && user.level instanceof net.minecraft.world.server.ServerWorld) {
                long now = ((net.minecraft.world.server.ServerWorld) user.level).getServer().overworld().getGameTime();
                net.minecraft.nbt.CompoundNBT data = user.getPersistentData();
                if (user instanceof PlayerEntity) {
                    if (!data.contains(PlayerEntity.PERSISTED_NBT_TAG, 10))
                        data.put(PlayerEntity.PERSISTED_NBT_TAG, new net.minecraft.nbt.CompoundNBT());
                    data = data.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
                }
                String key = "kazimod:ea_draw_next_cutscene";
                if (net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutsceneCooldown.ready(now, data.getLong(key))) {
                    data.putLong(key, net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutsceneCooldown.nextAllowedTick(now));
                    net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutscenes.start(user, drawSequence.startTick(), true);
                } else drawSequence.stop();
            }
        }
        super.tick(user);
    }

    public static boolean isEligible(LivingEntity user) {
        if (user == null) return false;
        IDevilFruit fruit = DevilFruitCapability.get(user);
        return fruit != null && fruit.hasAwakenedFruit()
                && fruit.hasDevilFruit(CartAbilities.GORU_GORU_NO_MI);
    }

    /**
     * Read this when selecting an awakening ability's form. It follows current health
     * and max health immediately, including healing, without retaining a stage after death.
     * An unavailable, disabled or paused passive always selects the normal form.
     */
    public static SeriousnessStage getStage(LivingEntity user) {
        if (getActivePassive(user) == null) return SeriousnessStage.NORMAL;
        return SeriousnessStage.fromHealth(user.getHealth(), user.getMaxHealth());
    }

    private static SeriousnessAbility getActivePassive(LivingEntity user) {
        if (user == null || !user.isAlive() || user.isSpectator() || !isEligible(user)) return null;
        IAbilityData data = AbilityDataCapability.get(user);
        SeriousnessAbility passive = data == null ? null : data.getPassiveAbility(INSTANCE);
        return passive != null && !passive.disableComponent.isDisabled() && !passive.isPaused()
                ? passive : null;
    }

    private static int getHealthPercentage(LivingEntity user) {
        if (user == null) return 0;
        float health = user.getHealth();
        float maxHealth = user.getMaxHealth();
        if (!Float.isFinite(health) || !Float.isFinite(maxHealth) || maxHealth <= 0.0F) return 0;
        return (int) Math.round(Math.max(0.0D, Math.min(1.0D, (double) health / maxHealth)) * 100.0D);
    }

    @OnlyIn(Dist.CLIENT)
    private void renderGauge(PlayerEntity player, MatrixStack matrixStack, int posX, int posY,
                             SeriousnessAbility ability) {
        if (getActivePassive(player) == null) return;
        SeriousnessStage stage = getStage(player);
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.enableBlend();
        String label = getHealthPercentage(player) + "%";
        int colour = stage == SeriousnessStage.NORMAL ? 0xFFAAAAAA : 0xFFFFD45A;
        WyHelper.drawStringWithBorder(mc.font, matrixStack, label,
                posX + 16 - mc.font.width(label) / 2, posY - 25, colour);
        RenderSystem.disableBlend();
    }
}
