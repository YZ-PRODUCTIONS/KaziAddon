package net.kazi.kazimod.worldturtle;

import java.util.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.eventbus.api.Event;
import xyz.pixelatedw.mineminenomi.api.effects.ModEffect;

public final class WorldTurtleImmunity {
    private static final Map<Effect,Boolean> CONTROL=new IdentityHashMap<>();
    public static boolean blocks(LivingEntity user,EffectInstance effect){
        return effect!=null&&WorldTurtleHitboxes.active(user)&&CONTROL.computeIfAbsent(effect.getEffect(),WorldTurtleImmunity::isControl);
    }
    private static boolean isControl(Effect effect){
        ResourceLocation id=effect.getRegistryName();if(id==null)return false;
        String ns=id.getNamespace(),name=id.getPath();
        if(!ns.equals("mineminenomi")&&!ns.equals("cartaddon")&&!ns.equals("kazimod"))return false;
        if(name.contains("unconscious")||name.contains("knockout")||name.contains("stun")||name.contains("paralys")
                ||name.equals("movement_blocked")||name.equals("frozen")||name.equals("dizzy")||name.equals("bind")
                ||name.equals("candle_lock")||name.equals("candy_stuck")||name.equals("washed"))return true;
        return effect.getCategory()==EffectType.HARMFUL&&effect instanceof ModEffect
                &&(((ModEffect)effect).isBlockingRotations()||((ModEffect)effect).isBlockingSwings());
    }
    public static void applicable(PotionEvent.PotionApplicableEvent event){
        if(blocks(event.getEntityLiving(),event.getPotionEffect()))event.setResult(Event.Result.DENY);
    }
    public static void clear(LivingEntity user){
        if(!WorldTurtleHitboxes.active(user))return;
        for(EffectInstance effect:new ArrayList<>(user.getActiveEffects()))if(blocks(user,effect))user.removeEffect(effect.getEffect());
    }
}
