package net.kazi.kazimod.mixin.client;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;
import net.kazi.kazimod.ghoul.*;
import net.minecraft.util.text.*;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.screens.CharacterCreatorScreen;
@Mixin(value=CharacterCreatorScreen.class,remap=false)
public abstract class GhoulSelectionMixin {
    @Shadow private int page;
    @Shadow private int minkTextureId;
    @Shadow private List<AbilityCore<?>> topAbilities;
    @Shadow private ITextComponent label;
    @Shadow private boolean allowMinkRaceSelect;
    @Unique private Boolean ghoul$originalSelectionSetting;
    @Shadow protected abstract void createAbilitySlotButtons();
    @Shadow public abstract int getSelectedRaceId();
    @Inject(method="isOnMinkPage",at=@At("HEAD"),cancellable=true)
    private void ghoulVariants(CallbackInfoReturnable<Boolean> cir){if(GhoulFeatures.AVAILABLE&&page==1&&getSelectedRaceId()==10)cir.setReturnValue(true);}
    @Inject(method="applyMinkSwitcher",at=@At("HEAD"))
    private void ghoulChoice(CallbackInfo ci){
        if(ghoul$originalSelectionSetting==null)ghoul$originalSelectionSetting=allowMinkRaceSelect;
        allowMinkRaceSelect=GhoulFeatures.AVAILABLE&&page==1&&getSelectedRaceId()==10||ghoul$originalSelectionSetting;
    }
    @Inject(method="createAbilitySlotButtons",at=@At("HEAD"))
    private void ghoulMoves(CallbackInfo ci){if(GhoulFeatures.AVAILABLE&&page==1&&getSelectedRaceId()==10){minkTextureId=Math.floorMod(minkTextureId,4);topAbilities=new ArrayList<>();topAbilities.add(GhoulAbility.CORES[0]);for(int i=1;i<=4;i++)topAbilities.add(GhoulAbility.CORES[1+minkTextureId*4+i-1]);String t=GhoulState.TYPES[minkTextureId];label=new StringTextComponent("Ghoul: "+Character.toUpperCase(t.charAt(0))+t.substring(1));}}
    @Inject(method={"increaseMinkRace","decreaseMinkRace"},at=@At("TAIL"))
    private void ghoulRefresh(CallbackInfo ci){if(GhoulFeatures.AVAILABLE&&page==1&&getSelectedRaceId()==10)createAbilitySlotButtons();}
}
