MAMMOTH FORM REPLACEMENT

The full/guard and heavy/hybrid forms of Mine Mine no Mi's mammoth fruit
now use the supplied mammoth models. Other Zoan forms are unchanged.

EDITABLE MODELS
  mammoth_full_animated.bbmodel
  mammoth_hybrid_animated.bbmodel

Both files retain the original cubes and embedded pixel textures. Four
foot joints were added to the full form for planted-foot animation.
The hybrid contains mammoth attachments only. Minecraft renders the
wearer's current skin, including enabled skin layers and slim arms,
on the same animated joints. No fixed human skin is baked into the model.

ANIMATIONS
  Idle, walk, run/Trample, transformation, jump/airborne, landing,
  crouch, swim, ordinary attacks, and a rideable pose.
  Ancient Sweep: charged trunk wind-up, lateral sweep, recovery.
  Ancient Stomp: repeated front-leg lift, impact and final ground slam.
  Ancient Trunk Vacuum: sustained extended trunk and intake motion.
  Ancient Stampede: raised-trunk roar, wind-up and charging gait.
  Legacy Trunk Shot clips remain editable, but the old ability is replaced.

Ability release/impact events are synchronized to tracking players.
Form hitboxes and riding rules are unchanged. Full-form held items
are hidden; hybrid items follow
the animated hand, with separate classic/slim hand positioning.

COMBAT REWORK
  Full form: +75 maximum HP and 25% additional damage reduction.
  Hybrid: +30 maximum HP and 10% additional damage reduction.
  Existing armor, toughness and other form bonuses are retained.

  Stampede (full form): 60-tick charge, 100-tick rush, speed 2.3,
  6-block contact radius, 100 damage and 25-tick per-target hit interval,
  powerful knockback and 25% dizzy chance, modeled on Punk Corna Dio.
  No magnetic-item cost or bull transformation. The native Trample
  movement is suspended while Stampede or the new Stomp controls movement.
  Cooldown: 200 ticks (10 seconds).

  Sweep (either form): a moving wind crescent instead of the melee cone.
  Keeps the 40-tick charge, 160-tick cooldown and 20/15 full/hybrid damage.
  The projectile uses MMNM's native collision and Haki damage handling.

  Stomp (full form): 96 ticks, 15-block horizontal radius, 6-block vertical
  reach. Damaging pulses hold affected enemies with short refreshed locks.
  A final 72.5-80 damage shockwave launches targets outward and upward.
  The last wind-up leaves normal hit immunity time before the finisher.
  Interruption does not trigger the finisher; holds expire within 3 ticks.
  Cooldown stays 200 ticks. Ground rupture is visual, not block destruction.

  Vacuum (either form): 30-block range, 80-tick duration, 100-tick cooldown.
  Pulls visible nearby enemies toward the animated trunk with bounded
  velocity and inward-flowing mesh ribbons. Reuse cancels it.
  One positional wind loop follows the effect and stops with it.

Old Sweep, Stomp and Trunk Shot unlocks and equipped slots migrate on login.
Existing Mammoth users with Full Form unlocked also gain Ancient Stampede.
The roar uses the CC0 elephant trumpet recording from Wikimedia Commons.
Source and license: src/main/resources/assets/kazimod/sounds/mammoth_roar.LICENSE.txt

PREVIEWS AND VERIFICATION
  mammoth_animations.png: textured poses; hybrid uses a sample Steve skin.
  mammoth_walk_run.gif: looping movement and attack clip preview.
  mammoth_combat_vfx.png: ground tremors, wind crescent and vacuum airflow.
  tools/MammothCheck.java: headless geometry, UV, clip, foot-contact,
  transition, per-player state, hand-alignment and preview checks.
  tools/MammothCombatCheck.java: range boundaries, damage curves, suction,
  animated trunk endpoint and effect vertex budgets.

The checks exercise the actual exported rigs and animation controller.
They do not replace an in-game check of equipment, multiplayer timing,
lighting, riding and interactions with other renderer mods.

Use the updated mod on both client and server. The network protocol
was advanced for the new synchronized mammoth animation message.

Source export: tools/import_mammoth.cjs
Texture assets: src/main/resources/assets/kazimod/textures/models/zoan/
Runtime rigs: src/main/resources/assets/kazimod/models/zoan/
