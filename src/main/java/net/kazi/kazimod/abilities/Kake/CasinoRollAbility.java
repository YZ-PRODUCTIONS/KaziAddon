package net.kazi.kazimod.abilities.Kake;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityType;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SwingTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import net.kazi.kazimod.init.KaziEffects;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.kazi.kazimod.particles.LuckySlotParticleEffect;
import net.kazi.kazimod.entities.GiantDiceEntity;
import net.kazi.kazimod.entities.projectiles.CasinoChipProjectile;
import net.kazi.kazimod.entities.projectiles.CoinProjectile;
import net.kazi.kazimod.entities.projectiles.DiceProjectile;
import net.kazi.kazimod.entities.projectiles.PlayingCardProjectile;

import java.util.List;

public class CasinoRollAbility extends Ability {

    public enum Mode {
        NONE("None"), COIN_FLICK("Coin Flick"), LOADED_DICE("Loaded Dice"),
        CARD_SLASH("Card Slash"), DOUBLE_DOWN("Double Down"), JACKPOT_SHOT("Jackpot Shot"),
        CASINO_CHIP_RAIN("Casino Chip Rain"), LUCKY_SEVEN("Lucky Seven"),
        CASINO_STORM("Casino Storm"), JACKPOT("JACKPOT");
        private final String d; Mode(String n){d=n;}
        public ITextComponent getDisplayName(){return new StringTextComponent(d);}
    }

    private enum ActiveMode { NONE, JACKPOT_SHOT, CHIP_RAIN, STORM, JACKPOT }

    private static final float COOLDOWN           = 300.0f;
    private static final float STORM_DAMAGE       = 20.0f;
    private static final float JACKPOT_DAMAGE     = 40.0f;
    private static final float LUCKY_SEVEN_RADIUS = 30.0f;
    private static final float JACKPOT_RADIUS     = 30.0f;
    private static final int   JACKPOT_BUFF_TICKS = 200;
    private static final float JACKPOT_SHOT_DUR   = 200.0f;
    private static final float CHIP_RAIN_DUR      = 240.0f;
    private static final float STORM_DUR          = 600.0f;
    private static final float JACKPOT_DUR        = 900.0f;
    private static final float COIN_FLICK_DAMAGE  = 40.0f;
    private static final float LOADED_DICE_DAMAGE = 40.0f;
    private static final float CARD_SLASH_DAMAGE  = 60.0f;
    private static final float JACKPOT_SHOT_DAMAGE = 15.0f;

    private static final double STORM_RADIUS    = 30.0;
    private static final double STORM_RADIUS_SQ = STORM_RADIUS * STORM_RADIUS;
    private static final int    STORM_POINTS    = 16;
    private static final double[] STORM_COS     = new double[STORM_POINTS];
    private static final double[] STORM_SIN     = new double[STORM_POINTS];
    static {
        for (int i = 0; i < STORM_POINTS; i++) {
            double a = i * (2.0 * Math.PI / STORM_POINTS);
            STORM_COS[i] = Math.cos(a); STORM_SIN[i] = Math.sin(a);
        }
    }

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "casino_roll",
            new Pair[]{ ImmutablePair.of(
                    "Activates based on your Lucky Slot roll (1-9). Each number triggers a unique casino attack.",
                    (Object) null) }
    );
    public static final AbilityCore<CasinoRollAbility> INSTANCE;

    private Mode currentMode = Mode.NONE;
    private final RangeComponent         rangeComponent;
    private final DealDamageComponent    damageComponent;
    private final ProjectileComponent    coinProjectile;
    private final ProjectileComponent    diceProjectile;
    private final ProjectileComponent    cardProjectile;
    private final ContinuousComponent    continuousComponent;
    private final SwingTriggerComponent  swingTrigger;

    private ActiveMode    activeMode  = ActiveMode.NONE;
    private int           contTick    = 0;
    private Vector3d      stormOrigin = null;
    private AxisAlignedBB stormBox    = null;

    public CasinoRollAbility(AbilityCore<CasinoRollAbility> core) {
        super(core);
        this.isNew = true;

        rangeComponent  = new RangeComponent(this);
        damageComponent = new DealDamageComponent(this);
        coinProjectile  = new ProjectileComponent(this, s -> new CoinProjectile(s.level, s));
        diceProjectile  = new ProjectileComponent(this, s -> new DiceProjectile(s.level, s));
        cardProjectile  = new ProjectileComponent(this, s -> new PlayingCardProjectile(s.level, s));

        continuousComponent = new ContinuousComponent(this, true);
        continuousComponent.addTickEvent(this::onContinuousTick);
        continuousComponent.addEndEvent(this::onContinuousEnd);

        swingTrigger = new SwingTriggerComponent(this);
        swingTrigger.addSwingEvent(this::onSwing);

        super.addComponents(new AbilityComponent[]{
                rangeComponent, damageComponent,
                coinProjectile, diceProjectile, cardProjectile,
                continuousComponent, swingTrigger
        });
        super.addUseEvent(this::onUse);
    }

    private void onUse(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (continuousComponent.isContinuous()) { continuousComponent.stopContinuity(entity); return; }

        Mode mode = currentMode;
        if (mode == Mode.NONE) return;

        IAbilityData data = AbilityDataCapability.get(entity);
        if (data != null) {
            LuckySlotAbility ls = data.getPassiveAbility(LuckySlotAbility.INSTANCE);
            if (ls != null && !ls.hasRolled()) {
                currentMode = Mode.NONE;
                return;
            }
        }

        switch (mode) {
            case COIN_FLICK:       doCoinFlick(entity);      break;
            case LOADED_DICE:      doLoadedDice(entity);     break;
            case CARD_SLASH:       doCardSlash(entity);      break;
            case DOUBLE_DOWN:      doDoubleDown(entity);     break;
            case JACKPOT_SHOT:     startJackpotShot(entity); break;
            case CASINO_CHIP_RAIN: startChipRain(entity);    break;
            case LUCKY_SEVEN:      doLuckySeven(entity);     break;
            case CASINO_STORM:     startCasinoStorm(entity); break;
            case JACKPOT:          startJackpot(entity);     break;
            default: break;
        }

        clearSlotBar(entity);
        currentMode = Mode.NONE;
        setDisplayName(Mode.NONE.getDisplayName());

        if (mode != Mode.JACKPOT_SHOT && mode != Mode.CASINO_CHIP_RAIN
                && mode != Mode.CASINO_STORM && mode != Mode.JACKPOT) {
            cooldownComponent.startCooldown(entity, COOLDOWN);
        }
    }

    private void onContinuousTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        contTick++;
        switch (activeMode) {
            case JACKPOT_SHOT: tickJackpotShot(entity); break;
            case CHIP_RAIN: tickChipRain(entity); break;
            case STORM:     tickStorm(entity);    break;
            case JACKPOT:   tickJackpot(entity);  break;
            default: break;
        }
    }

    private void onContinuousEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (activeMode == ActiveMode.STORM) { stormOrigin = null; stormBox = null; }
        if (activeMode == ActiveMode.JACKPOT) entity.getPersistentData().putBoolean("kazi_jackpot_dmg_buff", false);
        contTick = 0; activeMode = ActiveMode.NONE;
        cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    private void onSwing(LivingEntity entity, IAbility ability) {
        // Jackpot Shot fires continuously in onContinuousTick, not on swing
    }

    private void doCoinFlick(LivingEntity entity) {
        CoinProjectile p = new CoinProjectile(entity.level, entity);
        p.setDamage(COIN_FLICK_DAMAGE);
        coinProjectile.shoot(p, entity, 3.5f, 0.5f);
    }

    private void doLoadedDice(LivingEntity entity) {
        for (int i = 0; i < 2; i++) {
            DiceProjectile d = new DiceProjectile(entity.level, entity);
            d.setDamage(LOADED_DICE_DAMAGE);
            d.setMaxLife(60); d.setGravity(0.06f);
            diceProjectile.shoot(d, entity, 2.0f, (i==0)?-5f:5f);
        }
    }

    private void doCardSlash(LivingEntity entity) {
        for (int i = 0; i < 3; i++) {
            PlayingCardProjectile c = new PlayingCardProjectile(entity.level, entity);
            c.setDamage(CARD_SLASH_DAMAGE);
            c.setMaxLife(50); c.setPassThroughEntities();
            cardProjectile.shoot(c, entity, 2.5f, (i-1)*8f);
        }
    }

    private void doDoubleDown(LivingEntity entity) {
        entity.getPersistentData().putBoolean("kazi_double_down", true);
    }

    private void startJackpotShot(LivingEntity entity) {
        activeMode = ActiveMode.JACKPOT_SHOT; contTick = 0;
        continuousComponent.startContinuity(entity, JACKPOT_SHOT_DUR);
    }

    private void tickJackpotShot(LivingEntity entity) {
        // Fire a coin projectile every tick toward the look direction, like Kaminari's beam
        // Spread slightly each tick for a shotgun feel
        CoinProjectile coin = new CoinProjectile(entity.level, entity);
        coin.setDamage(JACKPOT_SHOT_DAMAGE);
        coin.setMaxLife(40);
        coinProjectile.shoot(coin, entity, 3.2f, (random.nextFloat()-0.5f)*8f);
    }

    private void startChipRain(LivingEntity entity) {
        activeMode = ActiveMode.CHIP_RAIN; contTick = 0;
        continuousComponent.startContinuity(entity, CHIP_RAIN_DUR);
    }

    private void tickChipRain(LivingEntity entity) {
        if (contTick % 4 != 0) return;
        double cx = entity.getX(), cy = entity.getY(), cz = entity.getZ();
        for (int i = 0; i < 10; i++) {
            double ox = (random.nextDouble()-0.5)*40.0;
            double oz = (random.nextDouble()-0.5)*40.0;
            CasinoChipProjectile chip = new CasinoChipProjectile(entity.level, entity);
            chip.setMaxLife(80);
            chip.moveTo(cx+ox, cy+20, cz+oz, 0, 90);
            entity.level.addFreshEntity(chip);
            chip.setDeltaMovement(0, -0.8, 0);
        }
    }

    private void doLuckySeven(LivingEntity entity) {
        if (entity.level.isClientSide) return;
        List<LivingEntity> targets = entity.level.getEntitiesOfClass(LivingEntity.class,
                new AxisAlignedBB(entity.getX()-LUCKY_SEVEN_RADIUS, entity.getY()-2, entity.getZ()-LUCKY_SEVEN_RADIUS,
                        entity.getX()+LUCKY_SEVEN_RADIUS, entity.getY()+10, entity.getZ()+LUCKY_SEVEN_RADIUS),
                t -> t!=entity && t.isAlive());
        for (LivingEntity t : targets)
            entity.level.addFreshEntity(new GiantDiceEntity(entity.level, entity, t));
    }

    private void startCasinoStorm(LivingEntity entity) {
        stormOrigin = entity.position();
        double bx = stormOrigin.x, by = entity.getY(), bz = stormOrigin.z;
        stormBox = new AxisAlignedBB(bx-STORM_RADIUS, by-4, bz-STORM_RADIUS, bx+STORM_RADIUS, by+36, bz+STORM_RADIUS);
        activeMode = ActiveMode.STORM; contTick = 0;
        continuousComponent.startContinuity(entity, STORM_DUR);
    }

    private void tickStorm(LivingEntity entity) {
        if (stormOrigin == null) return;
        double bx = stormOrigin.x, by = stormOrigin.y, bz = stormOrigin.z;

        if (contTick % 2 == 0 && stormBox != null) {
            entity.level.getEntitiesOfClass(LivingEntity.class, stormBox, t -> t!=entity && t.isAlive())
                    .forEach(t -> {
                        double dx = t.getX()-bx, dz = t.getZ()-bz;
                        if (dx*dx + dz*dz < STORM_RADIUS_SQ) return;
                        AbilityHelper.setDeltaMovement(t, new Vector3d(bx, t.getY(), bz).subtract(t.position()).normalize());
                    });
        }

        if (contTick % 4 == 0) {
            ParticleEffect<?> cardFx = (ParticleEffect<?>) KaziParticleEffects.PLAYING_CARD.get();
            double[] yLayers = { by+1, by+9, by+17 };
            for (double ry : yLayers) {
                for (int i = 0; i < STORM_POINTS; i++) {
                    WyHelper.spawnParticleEffect(cardFx, entity,
                            bx + STORM_RADIUS*STORM_COS[i], ry,
                            bz + STORM_RADIUS*STORM_SIN[i]);
                }
            }
        }

        if (contTick % 3 == 0) {
            for (int i = 0; i < 6; i++) {
                double ox = (random.nextDouble()-0.5)*STORM_RADIUS*1.8;
                double oz = (random.nextDouble()-0.5)*STORM_RADIUS*1.8;
                PlayingCardProjectile card = new PlayingCardProjectile(entity.level, entity);
                card.setDamage(CARD_SLASH_DAMAGE * 0.5f);
                card.setMaxLife(50); card.setGravity(0.05f);
                card.moveTo(bx+ox, by+24, bz+oz, 0, 90);
                entity.level.addFreshEntity(card);
                card.setDeltaMovement((random.nextDouble()-0.5)*1.0, -0.8-random.nextDouble(), (random.nextDouble()-0.5)*1.0);
            }
        }

        if (contTick % 8 == 0) {
            for (int i = 0; i < 3; i++) {
                double ox = (random.nextDouble()-0.5)*STORM_RADIUS*1.6;
                double oz = (random.nextDouble()-0.5)*STORM_RADIUS*1.6;
                CoinProjectile coin = new CoinProjectile(entity.level, entity);
                coin.setDamage(STORM_DAMAGE);
                coin.setMaxLife(55); coin.setGravity(0.08f);
                coin.moveTo(bx+ox, by+24, bz+oz, 0, 90);
                entity.level.addFreshEntity(coin);
                coin.setDeltaMovement(0, -0.9, 0);
            }
            for (int i = 0; i < 4; i++) {
                double ox = (random.nextDouble()-0.5)*STORM_RADIUS*1.6;
                double oz = (random.nextDouble()-0.5)*STORM_RADIUS*1.6;
                CasinoChipProjectile chip = new CasinoChipProjectile(entity.level, entity);
                chip.setDamage(STORM_DAMAGE);
                chip.setMaxLife(55); chip.setGravity(0.08f);
                chip.moveTo(bx+ox, by+24, bz+oz, 0, 90);
                entity.level.addFreshEntity(chip);
                chip.setDeltaMovement((random.nextDouble()-0.5)*0.3, -0.9, (random.nextDouble()-0.5)*0.3);
            }

            entity.level.getEntitiesOfClass(LivingEntity.class,
                            new AxisAlignedBB(bx-STORM_RADIUS, by-2, bz-STORM_RADIUS, bx+STORM_RADIUS, by+26, bz+STORM_RADIUS),
                            t -> t!=entity && t.isAlive())
                    .forEach(t -> damageComponent.hurtTarget(entity, t, STORM_DAMAGE*0.3f));
        }
    }

    private void startJackpot(LivingEntity entity) {
        entity.getPersistentData().putBoolean("kazi_jackpot_dmg_buff", true);
        entity.getPersistentData().putInt("kazi_jackpot_dmg_ticks", JACKPOT_BUFF_TICKS);
        activeMode = ActiveMode.JACKPOT; contTick = 0;
        continuousComponent.startContinuity(entity, JACKPOT_DUR);
    }

    private void tickJackpot(LivingEntity entity) {
        // Refresh buffs every tick
        entity.addEffect(new EffectInstance(KaziEffects.ENHANCED_MOVEMENT.get(), 10, 0, false, false));
        entity.addEffect(new EffectInstance(net.minecraft.potion.Effects.REGENERATION, 10, 1, false, false));
        entity.addEffect(new EffectInstance(net.minecraft.potion.Effects.DAMAGE_RESISTANCE, 10, 0, false, false));

        // Coin eruption every 5 ticks
        if (contTick % 5 == 0) {
            for (int i = 0; i < 3; i++) {
                CoinProjectile coin = new CoinProjectile(entity.level, entity);
                coin.setDamage(JACKPOT_DAMAGE * 0.15f);
                coin.setMaxLife(30); coin.setGravity(0.04f);
                double yaw = random.nextDouble()*Math.PI*2, pitch = (random.nextDouble()-0.3)*Math.PI*0.4;
                coin.moveTo(entity.getX(), entity.getY()+1.0, entity.getZ(), 0, 0);
                entity.level.addFreshEntity(coin);
                coin.setDeltaMovement(Math.cos(yaw)*Math.cos(pitch)*1.5, Math.sin(pitch)+0.3, Math.sin(yaw)*Math.cos(pitch)*1.5);
            }
            for (int i = 0; i < 2; i++) {
                CasinoChipProjectile chip = new CasinoChipProjectile(entity.level, entity);
                chip.setDamage(JACKPOT_DAMAGE * 0.15f);
                chip.setMaxLife(30); chip.setGravity(0.04f);
                double yaw = random.nextDouble()*Math.PI*2, pitch = (random.nextDouble()-0.3)*Math.PI*0.4;
                chip.moveTo(entity.getX(), entity.getY()+1.0, entity.getZ(), 0, 0);
                entity.level.addFreshEntity(chip);
                chip.setDeltaMovement(Math.cos(yaw)*Math.cos(pitch)*1.5, Math.sin(pitch)+0.3, Math.sin(yaw)*Math.cos(pitch)*1.5);
            }
        }

        // Card eruption every 15 ticks
        if (contTick % 15 == 0) {
            for (int i = 0; i < 6; i++) {
                PlayingCardProjectile card = new PlayingCardProjectile(entity.level, entity);
                card.setDamage(JACKPOT_DAMAGE * 0.2f);
                card.setMaxLife(40); card.setGravity(0.02f);
                double yaw = (i/6.0)*Math.PI*2;
                card.moveTo(entity.getX(), entity.getY()+1.2, entity.getZ(), 0, 0);
                entity.level.addFreshEntity(card);
                card.setDeltaMovement(Math.cos(yaw)*1.2, 0.4, Math.sin(yaw)*1.2);
            }
        }

        // AoE damage every 20 ticks — no dizzy, no knockback
        if (contTick % 20 == 0) {
            entity.level.getEntitiesOfClass(LivingEntity.class,
                            new AxisAlignedBB(entity.getX()-JACKPOT_RADIUS, entity.getY()-2, entity.getZ()-JACKPOT_RADIUS,
                                    entity.getX()+JACKPOT_RADIUS, entity.getY()+10, entity.getZ()+JACKPOT_RADIUS),
                            t -> t!=entity && t.isAlive())
                    .forEach(t -> damageComponent.hurtTarget(entity, t, JACKPOT_DAMAGE*0.05f));
        }
    }

    private void clearSlotBar(LivingEntity entity) {
        IAbilityData data = AbilityDataCapability.get(entity);
        if (data != null) {
            LuckySlotAbility ls = data.getPassiveAbility(LuckySlotAbility.INSTANCE);
            if (ls != null) ls.setSlotNumber(entity, -1);
        }
    }

    public void setModeForRoll(LivingEntity entity, int roll) {
        switch (roll) {
            case 1: currentMode=Mode.COIN_FLICK; break; case 2: currentMode=Mode.LOADED_DICE; break;
            case 3: currentMode=Mode.CARD_SLASH; break; case 4: currentMode=Mode.DOUBLE_DOWN; break;
            case 5: currentMode=Mode.JACKPOT_SHOT; break; case 6: currentMode=Mode.CASINO_CHIP_RAIN; break;
            case 7: currentMode=Mode.LUCKY_SEVEN; break; case 8: currentMode=Mode.CASINO_STORM; break;
            case 9: currentMode=Mode.JACKPOT; break; default: currentMode=Mode.NONE; break;
        }
        setDisplayName(currentMode.getDisplayName());
    }

    public boolean isStormActive()   { return activeMode==ActiveMode.STORM   && continuousComponent.isContinuous(); }
    public boolean isJackpotActive() { return activeMode==ActiveMode.JACKPOT && continuousComponent.isContinuous(); }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Casino Roll", AbilityCategory.DEVIL_FRUITS,
                AbilityType.ACTION, CasinoRollAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceElement(SourceElement.NONE)
                .setSourceType(SourceType.PROJECTILE, SourceType.UNKNOWN)
                .build();
    }
}