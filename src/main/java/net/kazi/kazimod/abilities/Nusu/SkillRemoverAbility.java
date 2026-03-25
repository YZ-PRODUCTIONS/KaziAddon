package net.kazi.kazimod.abilities.Nusu;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

import java.util.List;

public class SkillRemoverAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "skill_remover",
            new Pair[]{
                    ImmutablePair.of(
                            "Requires the Skill Book in hand. Opens a menu to return stolen abilities to their owners.",
                            (Object) null)
            });

    public static final AbilityCore<SkillRemoverAbility> INSTANCE;

    public SkillRemoverAbility(AbilityCore<SkillRemoverAbility> core) {
        super(core);
        this.isNew = true;
        this.addUseEvent(this::onUse);
    }

    private void onUse(LivingEntity user, IAbility ability) {
        if (!(user instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) user;

        // Require skill book in hand
        if (!SkillHunterAbility.hasSkillBook(user)) {
            sendMsg(player, "\u00a7cYou need the Skill Book in your hand");
            return;
        }

        List<AbilityCore<?>> held = NusuStolenData.getHeld(user);
        if (held.isEmpty()) {
            sendMsg(player, "\u00a7eYou have no stolen abilities to remove.");
            return;
        }

        sendMsg(player, "\u00a76--- Skill Remover - Click to return ---");
        for (int i = 0; i < held.size(); i++) {
            AbilityCore<?> core = held.get(i);
            String name = core.getLocalizedName().getString();
            int slotNum = i + 1;
            StringTextComponent line = new StringTextComponent(
                    "\u00a7e[" + slotNum + "] \u00a7c" + name + " \u00a77(click to return)");
            line.withStyle(style -> style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nusu_remove " + slotNum))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new StringTextComponent("Return " + name + " to its owner"))));
            player.sendMessage(line, player.getUUID());
        }
        sendMsg(player, "\u00a76----------------------------------------");
    }

    public static void removeBySlot(PlayerEntity player, int slot) {
        // Require skill book in hand for the command too
        if (!SkillHunterAbility.hasSkillBook(player)) {
            sendMsg(player, "\u00a7cYou need the Skill Book in your hand");
            return;
        }

        int index = slot - 1;
        List<AbilityCore<?>> held = NusuStolenData.getHeld(player);

        if (index < 0 || index >= held.size()) {
            sendMsg(player, "\u00a7cInvalid slot number. You have " + held.size() + " stolen abilities.");
            return;
        }

        AbilityCore<?> core = held.get(index);
        String name = core.getLocalizedName().getString();

        LivingEntity victim = null;
        if (player.level != null) {
            for (PlayerEntity p : player.level.players()) {
                if (NusuStolenData.isStolen(p, core)) {
                    victim = p;
                    break;
                }
            }
        }

        if (victim != null) {
            NusuEvents.returnToVictim(victim, core);
            IAbilityData nusuData = AbilityDataCapability.get(player);
            if (nusuData != null) nusuData.removeUnlockedAbility(core);
            NusuStolenData.removeHeld(player, core);
            sendMsg(player, "\u00a7a" + name + " returned to " + victim.getName().getString());
            NusuEvents.sendMsg(victim, "\u00a7a" + name + " has been returned to you");
        } else {
            NusuEvents.discardAbility(player, core);
            sendMsg(player, "\u00a7e" + name + " discarded (victim is offline)");
        }
    }

    private static void sendMsg(PlayerEntity player, String text) {
        player.sendMessage(new StringTextComponent(text), player.getUUID());
    }

    static {
        INSTANCE = new AbilityCore.Builder<SkillRemoverAbility>(
                "Skill Remover", AbilityCategory.DEVIL_FRUITS, SkillRemoverAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE
                })
                .build();
    }
}