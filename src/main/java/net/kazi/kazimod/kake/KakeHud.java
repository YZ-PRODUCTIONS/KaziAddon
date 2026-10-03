package net.kazi.kazimod.kake;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.entity.player.PlayerEntity;
import net.kazi.kazimod.abilities.Kake.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;

public final class KakeHud {
    public static void render(PlayerEntity player,MatrixStack stack,int x,int y,int result){
        Minecraft mc=Minecraft.getInstance();int top=y-38;
        SlotSpinAbility spin=AbilityDataCapability.get(player).getEquippedAbility(SlotSpinAbility.INSTANCE);
        boolean spinning=spin!=null&&spin.isSpinning();long time=player.level.getGameTime();
        AbstractGui.fill(stack,x,top+2,x+32,top+29,0xffd9a638);
        AbstractGui.fill(stack,x+2,top+4,x+30,top+26,0xff912641);
        AbstractGui.fill(stack,x+3,top+8,x+29,top+21,0xff161b2b);
        for(int i=0;i<3;i++){
            AbstractGui.fill(stack,x+4+i*9,top+9,x+11+i*9,top+20,0xfff5f4e9);
            String number=result<0?"-":Integer.toString(result);
            if(spinning)number=Long.toString((time*2+i*3)%10);
            int color=result>=7?0x168b7d:result==0?0xb9284f:0x161b2b;
            mc.font.draw(stack,number,x+5+i*9,top+10,color);
        }
        AbstractGui.fill(stack,x+10,top+23,x+22,top+25,0xff161b2b);
        AbstractGui.fill(stack,x+32,top+11,x+34,top+23,0xffc7cbd1);
        AbstractGui.fill(stack,x+31,top+8,x+35,top+12,0xff26c9ba);
        for(int i=0;i<5;i++)AbstractGui.fill(stack,x+3+i*6,top,x+5+i*6,top+2,(spinning&&(time+i)%3==0)?0xffffffff:0xffffcb58);
    }
}
