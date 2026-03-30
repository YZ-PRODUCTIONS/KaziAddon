package net.kazi.kazimod.abilities.HakiRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.haki.HakiDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.haki.IHakiData;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

import java.util.List;
import java.util.stream.Collectors;

public class HakiSenseAbility extends Ability {
    private static final ResourceLocation ICON =
            new ResourceLocation("kazimod", "textures/abilities/haki_sense.png");

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod",
            "haki_sense",
            new Pair[]{ImmutablePair.of("Sense nearby presences with Haki. Stronger foes feel terrifying, weaker ones feel faint.", null)}
    );

    private static final float COOLDOWN_TICKS = 200.0F;
    private static final int RANGE = 2000;

    public static final AbilityCore<HakiSenseAbility> INSTANCE;

    private final CooldownComponent cooldownComponent = new CooldownComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);

    public HakiSenseAbility(AbilityCore<HakiSenseAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.cooldownComponent, this.rangeComponent});
        this.addUseEvent(this::onUseEvent);
        this.setDisplayIcon(ICON);
    }

    private void onUseEvent(LivingEntity user, IAbility ability) {
        if (!(user instanceof ServerPlayerEntity)) {
            return;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) user;
        player.level.playSound(
                null,
                player.blockPosition(),
                ModSounds.KENBUNSHOKU_HAKI_ON_SFX.get(),
                SoundCategory.PLAYERS,
                1.5F,
                1.0F
        );

        IEntityStats selfStats = EntityStatsCapability.get(player);
        if (selfStats == null) {
            return;
        }

        double myDoriki = selfStats.getDoriki();
        List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(player, RANGE)
                .stream()
                .filter(e -> e instanceof ServerPlayerEntity)
                .collect(Collectors.toList());

        boolean terrifyingNearby = false;
        for (LivingEntity target : targets) {
            if (target == player) {
                continue;
            }

            IEntityStats otherStats = EntityStatsCapability.get(target);
            if (otherStats == null) {
                continue;
            }

            double diff = otherStats.getDoriki() - myDoriki;
            double dist = player.distanceTo(target);
            String direction = getDirection(player, target);
            String msg = getPresenceMessage(diff, dist, direction, target.getName().getString());
            if (msg != null) {
                player.sendMessage(new StringTextComponent(msg), player.getUUID());
            }

            if (diff >= 5000) {
                terrifyingNearby = true;
            }
        }

        if (terrifyingNearby) {
            player.level.playSound(
                    null,
                    player.blockPosition(),
                    ModSounds.FUTURE_SIGHT_HIT.get(),
                    SoundCategory.PLAYERS,
                    1.2F,
                    0.8F
            );
        }

        this.cooldownComponent.startCooldown(player, COOLDOWN_TICKS);
    }

    private String getPresenceMessage(double diff, double dist, String direction, String name) {
        int distance = (int) dist;

        if (diff <= -5000) return "§a[Weak] §7" + name + " sensed " + distance + " blocks away to the " + direction;
        if (diff <= -1000) return "§2[Faint] §7" + name + " sensed " + distance + " blocks away to the " + direction;
        if (diff < 1000) return "§7[Neutral] " + name + " sensed " + distance + " blocks away to the " + direction;
        if (diff < 5000) return "§c[Strong] §7" + name + " sensed " + distance + " blocks away to the " + direction;
        return "§4§l[Terrifying] §7" + name + " sensed " + distance + " blocks away to the " + direction;
    }

    private String getDirection(ServerPlayerEntity player, LivingEntity target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        if (angle < 0) {
            angle += 360.0D;
        }

        String[] directions = new String[]{
                "north",
                "north-east",
                "east",
                "south-east",
                "south",
                "south-west",
                "west",
                "north-west"
        };
        int index = (int) Math.round(angle / 45.0D) % 8;
        return directions[index];
    }

    private static boolean canUnlock(LivingEntity user) {
        IHakiData hakiProps = HakiDataCapability.get(user);
        IEntityStats statsProps = EntityStatsCapability.get(user);
        if (hakiProps == null || statsProps == null) {
            return false;
        }

        return statsProps.getDoriki() >= 1000.0D && hakiProps.getKenbunshokuHakiExp() >= 20.0F;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Haki Sense", AbilityCategory.HAKI, HakiSenseAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN_TICKS),
                        RangeComponent.getTooltip(RANGE, RangeType.AOE)
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{SourceType.INDIRECT})
                .setUnlockCheck(HakiSenseAbility::canUnlock)
                .setIcon(ICON)
                .build();
    }
}
