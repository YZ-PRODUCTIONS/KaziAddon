package net.kazi.kazimod.abilities.NagiRework;

import net.kazi.kazimod.entities.projectiles.SilentDeathProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class SilentDeathAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "silent_death",
                    new Pair[]{ImmutablePair.of(
                            "The user launches a deadly poison orb. Enemies hit are afflicted with slowness, doku poison, and blindness.", null)});

    private static final ResourceLocation ICON = new ResourceLocation("kazimod", "textures/abilities/silent_death.png");
    public static final float DAMAGE_VALUE = 10.0F;
    private static final float COOLDOWN = 600.0F; // 30 seconds

    public static final AbilityCore<SilentDeathAbility> INSTANCE;

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);

    public SilentDeathAbility(AbilityCore<SilentDeathAbility> core) {
        super(core);
        this.isNew = true;

        this.addComponents(new AbilityComponent[]{
                this.dealDamageComponent
        });

        this.addUseEvent(this::onUse);
    }

    private void onUse(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            Vector3d look = entity.getLookAngle();
            SilentDeathProjectile poison = new SilentDeathProjectile(entity.level, entity);
            poison.setPos(
                    entity.getX() + look.x * 1.5,
                    entity.getY() + 1.0 + look.y * 1.5,
                    entity.getZ() + look.z * 1.5
            );
            poison.setDeltaMovement(look.x * 3.0, look.y * 3.0, look.z * 3.0);
            entity.level.addFreshEntity(poison);
        }

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS, 1.0F, 0.6F);

        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Silent Death", AbilityCategory.DEVIL_FRUITS, SilentDeathAbility::new))
                .setIcon(ICON)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        DealDamageComponent.getTooltip(DAMAGE_VALUE)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .build();
    }
}
