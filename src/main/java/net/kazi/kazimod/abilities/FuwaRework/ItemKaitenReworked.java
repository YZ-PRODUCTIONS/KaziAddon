package net.kazi.kazimod.abilities.FuwaRework;

import net.minecraft.command.arguments.EntityAnchorArgument.Type;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.kazi.kazimod.entities.projectiles.ItemKaitenReworkedProjectile;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.StackComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ItemKaitenReworked extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "item_kaiten", new Pair[]{ImmutablePair.of("The user summons 3 orbiting rocks that can be tossed individually by swinging.", (Object)null)});
    private static final int COOLDOWN = 360;
    public static final AbilityCore<ItemKaitenReworked> INSTANCE;
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final StackComponent stackComponent = (new StackComponent(this, 3)).addStackChangeEvent(this::onStacksChange);
    private final ItemKaitenReworkedProjectile[] rocks = new ItemKaitenReworkedProjectile[3];
    private int firedIndex = 0;
    private boolean swingGate = false;
    private boolean rocksActive = false;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(this::startTickEvent).addTickEvent(this::onTickEvent).addEndEvent(this::endTickEvent);

    public ItemKaitenReworked(AbilityCore<ItemKaitenReworked> core) {
        super(core);
        super.isNew = true;
        this.addCanUseCheck(AbilityHelper::requiresOnGround);
        this.addComponents(new AbilityComponent[]{this.projectileComponent, this.stackComponent, this.continuousComponent});
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity);
    }

    private void startTickEvent(LivingEntity entity, IAbility ability) {
        if (!this.rocksActive && this.stackComponent.getStacks() == 3 && this.firedIndex == 0) {
            for(int i = 0; i < 3; ++i) {
                ItemKaitenReworkedProjectile rock = new ItemKaitenReworkedProjectile(entity.level, entity);
                rock.setOrbitOffset((float)(i * 120));
                double x = entity.getX();
                double y = entity.getY();
                double z = entity.getZ();
                if (!entity.level.isClientSide) {
                    WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.TAKT_EMERGENCE_IDLE.get(), entity, x, y, z);
                }

                rock.moveTo(x, y - (double)6.0F, z, 0.0F, 0.0F);
                rock.shoot((double)0.0F, (double)1.0F, (double)0.0F, 1.4F, 0.0F);
                entity.level.addFreshEntity(rock);
                this.rocks[i] = rock;
            }

            this.rocksActive = true;
        }

    }

    private void onTickEvent(LivingEntity entity, IAbility ability) {
        if (entity.swinging) {
            if (!this.swingGate && this.stackComponent.getStacks() > 0 && this.firedIndex < 3) {
                ItemKaitenReworkedProjectile rock = this.rocks[this.firedIndex];
                if (rock != null && rock.isAlive()) {
                    RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity, (double)80.0F);
                    Vector3d pos = mop.getLocation();
                    rock.lookAt(Type.FEET, pos);
                    Vector3d dir = rock.getLookAngle().scale((double)2.75F);
                    rock.setDeltaMovement(dir.x, dir.y, dir.z);
                    rock.setTossed(true);
                    ++this.firedIndex;
                    this.stackComponent.addStacks(entity, this, -1);
                }

                this.swingGate = true;
            }
        } else {
            this.swingGate = false;
        }

    }

    private void endTickEvent(LivingEntity entity, IAbility ability) {
        super.cooldownComponent.startCooldown(entity, 360.0F);

    }

    private void onStacksChange(LivingEntity entity, IAbility ability, int stacks) {
        if (stacks <= 0) {
            this.continuousComponent.stopContinuity(entity);
            this.stackComponent.revertStacksToDefault(entity, this);
            this.firedIndex = 0;
            this.rocksActive = false;
        }

    }

    private ItemKaitenReworkedProjectile createProjectile(LivingEntity entity) {
        return new ItemKaitenReworkedProjectile(entity.level, entity);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Item Kaiten", AbilityCategory.DEVIL_FRUITS, ItemKaitenReworked::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, StackComponent.getTooltip(3), AbilityHelper.createShortLongCooldownStat(0.0F, 360.0F)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).setSourceHakiNature(SourceHakiNature.SPECIAL).setIcon(new ResourceLocation("cartaddon", "textures/abilities/item_kaiten.png")).build();
    }
}
