package net.kazi.kazimod.mahoraga;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.helpers.RendererHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.packets.server.ability.SSyncAbilityPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public final class MahoragaAbility extends Ability {
    public static final AbilityCore<MahoragaAbility> INSTANCE = new AbilityCore.Builder<>(
            "Mahoraga", AbilityCategory.DEVIL_FRUITS, MahoragaAbility::new)
            .setUnlockCheck(MahoragaAbility::awakened)
            .setIcon(new ResourceLocation("kazimod", "textures/abilities/mahoraga.png"))
            .addDescriptionLine(new StringTextComponent("Kage awakening: summon an adaptive guardian that protects you and your teammates within 50 blocks. Use again to dismiss."))
            .addDescriptionLine(new StringTextComponent("After 10 seconds without a hit, each wheel turn adapts to ALL remembered attacks: +33% resistance per distinct attack and +15% per distinct element or type, up to immunity. Adaptations expire 30 minutes after first learned; Haki categories and special damage cannot be adapted to."))
            .addAdvancedDescriptionLine(CooldownComponent.getTooltip(1200), ContinuousComponent.getTooltip())
            .build();
    private final ContinuousComponent continuous = new ContinuousComponent(this);
    private float guardianHealth, guardianMaxHealth=700;
    private boolean guardianActive;
    public MahoragaAbility(AbilityCore<MahoragaAbility> core) {
        super(core);
        isNew = true;
        addComponents(continuous);
        if(isClientSide())addComponents(new GaugeComponent(this,this::renderGauge));
        addCanUseCheck((user, ability) -> continuous.isContinuous() || awakened(user)
                ? AbilityUseResult.success() : AbilityUseResult.fail(new StringTextComponent("Awaken Kage Kage no Mi first.")));
        addUseEvent((user, ability) -> {
            if (continuous.isContinuous()) continuous.stopContinuity(user);
            else if (!user.level.isClientSide) {
                MahoragaEntity summon = MahoragaFeatures.summon(user);
                if (summon != null) continuous.startContinuity(user);
            }
        });
        continuous.addTickEvent((user, ability) -> {
            if (!user.level.isClientSide && (!awakened(user) || !user.isAlive() || MahoragaFeatures.find(user) == null))
                continuous.stopContinuity(user);
        });
        continuous.addEndEvent((user, ability) -> {
            if (!user.level.isClientSide) {
                MahoragaFeatures.dismiss(user);
                cooldownComponent.startCooldown(user, 1200);
            }
        });
        addRemoveEvent((user, ability) -> {
            if (continuous.isContinuous()) continuous.stopContinuity(user);
            if (!user.level.isClientSide) MahoragaFeatures.dismiss(user);
        });
    }

    public void updateHud(PlayerEntity owner,float health,float maxHealth,boolean active) {
        health=Math.max(0,health);maxHealth=Math.max(1,maxHealth);
        if(Math.abs(guardianHealth-health)<.05F && Math.abs(guardianMaxHealth-maxHealth)<.05F && guardianActive==active)return;
        guardianHealth=health;guardianMaxHealth=maxHealth;guardianActive=active;
        WyNetwork.sendTo(new SSyncAbilityPacket(owner.getId(),this),owner);
    }

    @Override public CompoundNBT save(CompoundNBT nbt) {
        nbt=super.save(nbt);
        nbt.putFloat("MahoragaHealth",guardianHealth);
        nbt.putFloat("MahoragaMaxHealth",guardianMaxHealth);
        nbt.putBoolean("MahoragaActive",guardianActive);
        return nbt;
    }

    @Override public void load(CompoundNBT nbt) {
        super.load(nbt);
        guardianHealth=Math.max(0,nbt.getFloat("MahoragaHealth"));
        guardianMaxHealth=Math.max(1,nbt.contains("MahoragaMaxHealth")?nbt.getFloat("MahoragaMaxHealth"):700);
        guardianActive=nbt.getBoolean("MahoragaActive");
    }

    @OnlyIn(Dist.CLIENT)
    private void renderGauge(PlayerEntity player,MatrixStack stack,int x,int y,MahoragaAbility ability) {
        if(ability.guardianHealth<=0)return;
        Minecraft mc=Minecraft.getInstance();
        RendererHelper.drawAbilityIcon(INSTANCE,stack,x+4,y-42,0,24,24);
        int left=x+2,top=y-16,width=28;
        AbstractGui.fill(stack,left-1,top-1,left+width+1,top+7,0xff0b0d14);
        AbstractGui.fill(stack,left,top,left+width,top+6,0xff393944);
        float ratio=Math.min(1,ability.guardianHealth/ability.guardianMaxHealth);
        int color=ability.guardianActive?0xffd9455b:0xffc99b54;
        AbstractGui.fill(stack,left,top,left+Math.round(width*ratio),top+6,color);
        String label=Math.round(ratio*100)+"%";
        mc.font.drawShadow(stack,label,x+16-mc.font.width(label)/2.0F,y-9,0xfff5f1e9);
    }

    public static boolean awakened(LivingEntity user) {
        return DevilFruitCapability.get(user).hasAwakenedFruit()
                && DevilFruitCapability.get(user).hasDevilFruit(ModAbilities.KAGE_KAGE_NO_MI);
    }
}
