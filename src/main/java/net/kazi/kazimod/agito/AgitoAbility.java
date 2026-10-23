package net.kazi.kazimod.agito;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.mahoraga.MahoragaAbility;
import net.kazi.kazimod.shikigami.TenShadowsTaming;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.helpers.RendererHelper;
import xyz.pixelatedw.mineminenomi.packets.server.ability.SSyncAbilityPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public final class AgitoAbility extends Ability {
    public static final AbilityCore<AgitoAbility> INSTANCE=new AbilityCore.Builder<>(
            "Agito",AbilityCategory.DEVIL_FRUITS,AgitoAbility::new)
            .setUnlockCheck(MahoragaAbility::awakened)
            .setIcon(new ResourceLocation("kazimod","textures/abilities/agito.png"))
            .addDescriptionLine(new StringTextComponent("Kage awakening: summon Merged Beast Agito. Fights beside you with electrified fists and regenerates 1% of its maximum HP each second."))
            .addDescriptionLine(new StringTextComponent("Left-click your Agito to request a healing embrace. While held, you cannot move or use abilities and Agito cannot attack. Restores 2% of your maximum HP per second. Left-click again, or a hit on either of you, ends the embrace."))
            .addDescriptionLine(new StringTextComponent("First defeat Agito in a taming ritual. Its death removes taming. Cooldown: 10 seconds after dismissal; 20 minutes if defeated."))
            .addAdvancedDescriptionLine(CooldownComponent.getTooltip(200,24000),ContinuousComponent.getTooltip()).build();
    private final ContinuousComponent continuous=new ContinuousComponent(this,true);
    private float guardianHealth,guardianMaxHealth=500;
    private boolean guardianActive;

    public AgitoAbility(AbilityCore<AgitoAbility> core){
        super(core);isNew=true;addComponents(continuous);
        if(isClientSide())addComponents(new GaugeComponent(this,this::renderGauge));
        addCanUseCheck((user,ability)->continuous.isContinuous() || MahoragaAbility.awakened(user)
                ?AbilityUseResult.success():AbilityUseResult.fail(new StringTextComponent("Awaken Kage Kage no Mi first.")));
        addCanUseCheck((user,ability)->TenShadowsTaming.canUse(user,"agito",continuous.isContinuous()));
        addUseEvent((user,ability)->{
            if(continuous.isContinuous())continuous.stopContinuity(user);
            else if(!user.level.isClientSide && AgitoFeatures.summon(user)!=null)continuous.startContinuity(user);
        });
        continuous.addTickEvent((user,ability)->{
            if(!user.level.isClientSide && (!user.isAlive() || !MahoragaAbility.awakened(user) || AgitoFeatures.find(user)==null))
                continuous.stopContinuity(user);
        });
        continuous.addEndEvent((user,ability)->{
            if(user.level.isClientSide)return;
            boolean defeated=user.getPersistentData().getBoolean(AgitoFeatures.DEFEATED_TAG);
            user.getPersistentData().remove(AgitoFeatures.DEFEATED_TAG);
            AgitoFeatures.dismiss(user);
            cooldownComponent.startCooldown(user,TenShadowsTaming.endCooldown(user,"agito",defeated));
        });
        addRemoveEvent((user,ability)->{
            if(continuous.isContinuous())continuous.stopContinuity(user);
            if(!user.level.isClientSide)AgitoFeatures.dismiss(user);
        });
    }

    public void updateHud(PlayerEntity owner,float health,float maxHealth,boolean active){
        health=Math.max(0,health);maxHealth=Math.max(1,maxHealth);
        if(Math.abs(guardianHealth-health)<.05F && Math.abs(guardianMaxHealth-maxHealth)<.05F && guardianActive==active)return;
        guardianHealth=health;guardianMaxHealth=maxHealth;guardianActive=active;
        WyNetwork.sendTo(new SSyncAbilityPacket(owner.getId(),this),owner);
    }

    @Override public CompoundNBT save(CompoundNBT nbt){
        nbt=super.save(nbt);
        nbt.putFloat("AgitoHealth",guardianHealth);
        nbt.putFloat("AgitoMaxHealth",guardianMaxHealth);
        nbt.putBoolean("AgitoActive",guardianActive);
        return nbt;
    }

    @Override public void load(CompoundNBT nbt){
        super.load(nbt);
        guardianHealth=Math.max(0,nbt.getFloat("AgitoHealth"));
        guardianMaxHealth=Math.max(1,nbt.contains("AgitoMaxHealth")?nbt.getFloat("AgitoMaxHealth"):500);
        guardianActive=nbt.getBoolean("AgitoActive");
    }

    @OnlyIn(Dist.CLIENT)
    private void renderGauge(PlayerEntity player,MatrixStack stack,int x,int y,AgitoAbility ability){
        if(ability.guardianHealth<=0)return;
        int[] at=net.kazi.kazimod.shikigami.SummonHud.position(x,y);x=at[0];y=at[1];
        Minecraft mc=Minecraft.getInstance();
        RendererHelper.drawAbilityIcon(INSTANCE,stack,x+4,y-42,0,24,24);
        int left=x+2,top=y-16,width=28;
        AbstractGui.fill(stack,left-1,top-1,left+width+1,top+7,0xff0b0d14);
        AbstractGui.fill(stack,left,top,left+width,top+6,0xff393944);
        float ratio=Math.min(1,ability.guardianHealth/ability.guardianMaxHealth);
        int color=ability.guardianActive?0xffa468ed:0xff886b9c;
        AbstractGui.fill(stack,left,top,left+Math.round(width*ratio),top+6,color);
        String label=Math.round(ratio*100)+"%";
        mc.font.drawShadow(stack,label,x+16-mc.font.width(label)/2.0F,y-9,0xfff5f1e9);
    }
}
