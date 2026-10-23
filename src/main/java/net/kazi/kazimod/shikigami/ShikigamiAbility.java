package net.kazi.kazimod.shikigami;

import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.mahoraga.MahoragaAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.helpers.RendererHelper;
import xyz.pixelatedw.mineminenomi.packets.server.ability.SSyncAbilityPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public final class ShikigamiAbility extends Ability {
    private static final Map<ShikigamiKind,AbilityCore<ShikigamiAbility>> CORES=new EnumMap<>(ShikigamiKind.class);
    static {
        for(ShikigamiKind kind:ShikigamiKind.values())CORES.put(kind,new AbilityCore.Builder<ShikigamiAbility>(
                kind.title,AbilityCategory.DEVIL_FRUITS,core->new ShikigamiAbility(core,kind))
                .setUnlockCheck(MahoragaAbility::awakened)
                .setIcon(new ResourceLocation("kazimod","textures/abilities/"+kind.id+".png"))
                .addDescriptionLine(new StringTextComponent(kind.description))
                .addDescriptionLine(new StringTextComponent("Tamed summons: use again to dismiss. Saved health recovers by 1% per second while dismissed. Protection IV netherite-equivalent armor."))
                .addDescriptionLine(new StringTextComponent("First defeat this shadow in a taming ritual. If your tamed shadow dies, it must be tamed again after a 20-minute cooldown."))
                .addAdvancedDescriptionLine(CooldownComponent.getTooltip(200,24000),ContinuousComponent.getTooltip()).build());
    }
    public static AbilityCore<ShikigamiAbility> core(ShikigamiKind kind){return CORES.get(kind);}
    public final ShikigamiKind kind;
    private final ContinuousComponent continuous=new ContinuousComponent(this,true);
    private float health,maxHealth;
    private boolean active;
    public ShikigamiAbility(AbilityCore<ShikigamiAbility> core,ShikigamiKind kind){
        super(core);this.kind=kind;maxHealth=(float)kind.health;isNew=true;addComponents(continuous);
        if(isClientSide())addComponents(new GaugeComponent(this,this::renderGauge));
        addCanUseCheck((user,ability)->continuous.isContinuous()||MahoragaAbility.awakened(user)
                ?AbilityUseResult.success():AbilityUseResult.fail(new StringTextComponent("Awaken Kage Kage no Mi first.")));
        addCanUseCheck((user,ability)->TenShadowsTaming.canUse(user,kind.id,continuous.isContinuous()));
        addUseEvent((user,ability)->{
            if(continuous.isContinuous())continuous.stopContinuity(user);
            else if(!user.level.isClientSide&&ShikigamiFeatures.summon(user,kind)!=null)continuous.startContinuity(user);
        });
        continuous.addTickEvent((user,ability)->{
            if(!user.level.isClientSide&&(!user.isAlive()||!MahoragaAbility.awakened(user)||ShikigamiFeatures.find(user,kind)==null))continuous.stopContinuity(user);
        });
        continuous.addEndEvent((user,ability)->{
            if(user.level.isClientSide)return;
            boolean defeated=user.getPersistentData().getBoolean(kind.defeatedTag());
            user.getPersistentData().remove(kind.defeatedTag());ShikigamiFeatures.dismiss(user,kind);
            cooldownComponent.startCooldown(user,TenShadowsTaming.endCooldown(user,kind.id,defeated));
        });
        addRemoveEvent((user,ability)->{
            if(continuous.isContinuous())continuous.stopContinuity(user);
            if(!user.level.isClientSide)ShikigamiFeatures.dismiss(user,kind);
        });
    }
    public void updateHud(PlayerEntity owner,float hp,float max,boolean present){
        if(Math.abs(health-hp)<.05&&Math.abs(maxHealth-max)<.05&&active==present)return;
        health=Math.max(0,hp);maxHealth=Math.max(1,max);active=present;
        WyNetwork.sendTo(new SSyncAbilityPacket(owner.getId(),this),owner);
    }
    @Override public CompoundNBT save(CompoundNBT nbt){nbt=super.save(nbt);nbt.putFloat("SummonHealth",health);nbt.putFloat("SummonMax",maxHealth);nbt.putBoolean("SummonActive",active);return nbt;}
    @Override public void load(CompoundNBT nbt){super.load(nbt);health=Math.max(0,nbt.getFloat("SummonHealth"));maxHealth=Math.max(1,nbt.getFloat("SummonMax"));active=nbt.getBoolean("SummonActive");}
    @OnlyIn(Dist.CLIENT) private void renderGauge(PlayerEntity player,MatrixStack stack,int x,int y,ShikigamiAbility ability){
        if(ability.health<=0)return;
        int[] at=SummonHud.position(x,y);x=at[0];y=at[1];
        RendererHelper.drawAbilityIcon(getCore(),stack,x+4,y-42,0,24,24);
        AbstractGui.fill(stack,x+1,y-17,x+31,y-9,0xff10121b);
        AbstractGui.fill(stack,x+2,y-16,x+30,y-10,0xff3c3a46);
        float ratio=Math.min(1,ability.health/ability.maxHealth);
        int[] colors={0xffbbc8db,0xffec9db4,0xff9abd5f,0xffbb8b64};
        AbstractGui.fill(stack,x+2,y-16,x+2+Math.round(28*ratio),y-10,ability.active?colors[kind.ordinal()]:0xff817c89);
        String label=Math.round(ratio*100)+"%";
        Minecraft mc=Minecraft.getInstance();mc.font.drawShadow(stack,label,x+16-mc.font.width(label)/2F,y-9,0xffeeeeee);
    }
}
