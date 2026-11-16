package net.kazi.kazimod.ope;

import net.MrMagicalCart.cartaddon.abilities.opeextra.ReworkedRoomAbility;
import net.kazi.kazimod.abilities.OpeRework.KRoomAnesthesiaRework;
import net.kazi.kazimod.config.KaziConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

public final class OpeRoomMode extends AltModeComponent<OpeRoomMode.Mode> {
    public enum Mode { ROOM, K_ROOM }
    public static final ResourceLocation ICON = new ResourceLocation("cartaddon", "textures/abilities/kroom.png");

    public OpeRoomMode(IAbility room) {
        super(room, Mode.class, Mode.ROOM);
    }

    public static boolean unlocked(LivingEntity user) {
        return !KaziConfig.INSTANCE.disableFruitChanges.get()
                && DevilFruitCapability.get(user).hasDevilFruit(ModAbilities.OPE_OPE_NO_MI)
                && DevilFruitCapability.get(user).hasAwakenedFruit();
    }

    @Override public void setNextInCycle(LivingEntity user) {
        Ability room = (Ability)getAbility();
        if (room.isCharging() || room.isContinuous()) return;
        if (isMode(Mode.K_ROOM) || unlocked(user)) super.setNextInCycle(user);
    }

    public static boolean selected(IAbility room) {
        return room != null && room.getComponent(ModAbilityKeys.ALT_MODE)
                .map(mode -> mode instanceof OpeRoomMode && mode.getCurrentMode() == Mode.K_ROOM).orElse(false);
    }

    public static boolean isActive(LivingEntity user) {
        IAbilityData data = AbilityDataCapability.get(user);
        ReworkedRoomAbility room = data.getEquippedAbility(ReworkedRoomAbility.INSTANCE);
        if (selected(room) && room.isContinuous() && unlocked(user)) return true;
        // Law's encounter still drives its own standalone K-Room.
        KRoomAnesthesiaRework legacy = data.getEquippedAbility(KRoomAnesthesiaRework.INSTANCE);
        return legacy != null && legacy.isContinuous();
    }

    static void consume(LivingEntity user) {
        IAbilityData data = AbilityDataCapability.get(user);
        ReworkedRoomAbility room = data.getEquippedAbility(ReworkedRoomAbility.INSTANCE);
        if (selected(room) && room.isContinuous()) {
            room.getComponent(ModAbilityKeys.CONTINUOUS).ifPresent(component -> component.stopContinuity(user));
        } else {
            KRoomAnesthesiaRework legacy = data.getEquippedAbility(KRoomAnesthesiaRework.INSTANCE);
            if (legacy != null) legacy.setAbilityUsed(true);
        }
    }
}
