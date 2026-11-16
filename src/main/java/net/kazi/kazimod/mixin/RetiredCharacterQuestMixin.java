package net.kazi.kazimod.mixin;

import net.kazi.kazimod.events.RetiredCharacterOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.quests.Quest;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataBase;

@Mixin(value = QuestDataBase.class, remap = false)
public abstract class RetiredCharacterQuestMixin {
    @Inject(method = "addInProgressQuest", at = @At("HEAD"), cancellable = true)
    private void kazi$blockRetiredTrials(Quest quest, CallbackInfoReturnable<Boolean> cir) {
        if (quest != null && RetiredCharacterOptions.retiredTrial(quest.getCore())) cir.setReturnValue(false);
    }
}
