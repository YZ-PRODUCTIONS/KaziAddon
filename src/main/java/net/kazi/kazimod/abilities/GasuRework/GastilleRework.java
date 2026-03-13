//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.GasuRework;

import java.awt.Color;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.gasu.ShinokuniAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.math.VectorHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gasu.BigGastilleProjectile;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gasu.GastilleProjectile;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.init.ModMorphs;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class GastilleRework extends Ability {
    private static final ITextComponent[] DESCRIPTION;
    private static final int COOLDOWN = 140;
    public static final AbilityCore<GastilleRework> INSTANCE;
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);

    public GastilleRework(AbilityCore<GastilleRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.projectileComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        float projSpeed = 5.5F;
        if (((MorphInfo)ModMorphs.SHINOKUNI.get()).isActive(entity)) {
            projSpeed = 8.0F;
        }

        RayTraceResult mop = WyHelper.rayTraceBlocks(entity, (double)64.0F);
        double beamDistance = Math.sqrt(entity.distanceToSqr(mop.getLocation().x, mop.getLocation().y, mop.getLocation().z));
        float damage = 50.0F;
        float size = 0.25F;
        float length = 50.0F;
        Vector3d pos = VectorHelper.calculateRotationBasedOffsetPosition(entity.position(), (double)entity.yBodyRot, (double)0.5F, 1.15, 0.8);
        LightningEntity bolt = new LightningEntity(entity, pos.x, pos.y, pos.z, entity.yRot, entity.xRot, length + (float)beamDistance, projSpeed, this.getCore());
        bolt.setBlocksAffectedLimit(1508);
        bolt.setMaxLife(40);
        bolt.setDamage(damage);
        bolt.setExplosion(5, true, 0.3F);
        bolt.setSize(size);
        bolt.setBoxSizeDivision((double)1.0F);
        bolt.setColor(new Color(13397929));
        bolt.setAngle(100);
        bolt.setTargetTimeToReset(6000);
        bolt.disableExplosionKnockback();
        bolt.setBranches(1);
        bolt.setSegments(1);
        entity.level.addFreshEntity(bolt);
        this.cooldownComponent.startCooldown(entity, 140.0F);
    }

    private AbilityProjectileEntity createProjectile(LivingEntity entity) {
        return (AbilityProjectileEntity)(((MorphInfo)ModMorphs.SHINOKUNI.get()).isActive(entity) ? new BigGastilleProjectile(entity.level, entity) : new GastilleProjectile(entity.level, entity));
    }

    static {
        DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "gastille", new Pair[]{ImmutablePair.of("Shoots a beam of lit gas from the users mouth, that explodes on impact", (Object)null), ImmutablePair.of("If %s is active a bigger and more destructive laser will be shot.", new Object[]{ShinokuniAbility.INSTANCE})});
        INSTANCE = (new AbilityCore.Builder("Gastille", AbilityCategory.DEVIL_FRUITS, GastilleRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(140.0F)}).setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceElement(SourceElement.EXPLOSION).build();
    }
}
