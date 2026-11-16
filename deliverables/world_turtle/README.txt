World Turtle - Kame Kame no Mi awakening

Use Awakening Essence as a Kame Kame no Mi user, then equip World Turtle Form.
Existing awakening settings and player-kill requirements still apply.
Already-awakened Kame users receive the new unlock through normal fruit validation/login.

Double-tap jump to fly, steer with the normal Mine Mine no Mi Zoan flight controls,
and sprint for faster flight. Flight uses the existing stamina/recovery system.

The original 1,599 Blockbench cubes and embedded smoother texture are preserved.
Scale: 3x the exported model, approximately 35.5 blocks from flipper tip to tip.
Movement core: normal player size (0.6 wide, 1.8 tall); the Daibutsu-sized box is removed.
Visual scale remains unchanged. Third-person zoom: 36 blocks.
Combat uses eight fixed-size turtle-only hitboxes: shell, neck, head, four flippers,
and tail. Elephants and the carried world are excluded. The movement collider is
not used for direct melee/projectile targeting while transformed.
Shell hits are immune to damage. Other part hits use the owner's 50% reduction,
armor, shield and shared hurt cooldown. Projectile shell hits cannot bypass immunity.
Parts have no AI, movement collisions, block scans, loot or independent health.
Hitbox dimensions stay constant; centers follow position/facing, not limb animation.
Parts disappear on form exit,
death or loss of their owner; they are not saved to the world.
Transform outdoors; this form is intentionally far too large for normal buildings.
Held items and first-person humanoid arms/legs are hidden in this form.

Seven authored clips: idle, walk, fly, fast_fly, takeoff, land, bite.
Runtime blending adds smooth turning, head tracking and transitions.
The carried world stays attached to the turtle while banking.

world_turtle_animated.bbmodel is the editable animated model.
world_turtle_animations.png shows texture-sampled previews of the runtime geometry.

Re-export: node tools/import_world_turtle.cjs path/to/world_turtle.bbmodel
The script keeps source geometry/UVs/textures intact and exports the rig plus clips.
tools/WorldTurtleCheck.java validates the rig/motion and renders previews and the icon.

Build and offline visual checks do not replace an in-game multiplayer flight test.

Combat update:
- World Turtle Form adds 200 maximum HP and halves final incoming body damage.
  Shell impacts remain immune. MMNM/Cart/Kazi stuns and knockouts are rejected,
  including forced effect application, and existing control effects are cleared.
  Other fruit weaknesses and ordinary debuffs are not blanket-disabled.
- Players can stand on the ocean disc and are carried along with its movement.
  Its collision surface follows root tilt; jumping, walking off, form exit and
  teleports release riders. No extra platform entities or blocks are created.
- Supernova: five giant stellar cores form in a ring around the turtle for two
  seconds, then fire steerable beams for four seconds. Reuse cancels the ability.
  60-second cooldown, 20 damage per half-second pulse, shared hit deduplication,
  up to 128-block aim distance. Beams stop at blocks and do not destroy terrain.
  One effect entity renders all five stars; combined mesh stays below 16,000 vertices.
- Divine Shield: 5 seconds, use again to cancel, 20-second cooldown after ending.
  Tucks head/flippers/tail into the shell; golden forcefield blocks all damage.
  Returns 10% of direct attack damage and reflects projectiles without healing.
  Reflection cannot recursively trigger another reflected-damage chain.
- Destruction: aim at ground within 144 blocks, 60-second cooldown.
  A black portal drops a bomb. The blast expands to 96 blocks, deals up to
  80 damage with distance falloff, and develops a mushroom cloud and shockwaves.
  Blast damage uses MMNM's registered damage component and Haki IMBUING source.
  Existing vanilla hit immunity defers the single blast hit for up to 1 second;
  intentional dodges/shields are not retried. Reflected bombs use their new owner.
  The terrain crater is 48 blocks in radius and up to 20 blocks deep.
  Bomb, portal, and explosion visuals are twice their original dimensions.
  Terrain work continues after the cloud fades if needed.
  Camera-facing filtering skips hidden back-facing turtle surfaces in-world.
  Hollow Nuke-style direct air placement avoids individual block-break effects.
  Terrain work is capped globally per world at 32,768 scans / 4,096 removals per tick
  with a 6 ms time budget (one block operation can exceed the remaining budget).
  Protected areas, unbreakable/container blocks, disabled griefing, and Forge
  block-protection cancellation are respected. No forced chunk loads or item spam.

World Shaking Spin: 3-second steerable shell rush at 4.05 blocks/tick (three
times Dawn Whip's configured speed), 30-second cooldown, 60 impact damage at
most once per target per second. Damage immunity lasts only while spinning.
Swept shell checks cover actual travel between ticks, skip allies/owner, and
respect terrain cover and protected areas. Cancelling brakes the rush and
restores normal flight. Existing fixed body-part hitbox sizes are unchanged.

Performance: exposed geometry and UVs are preserved; 3,419 invisible faces removed.
24,700 submitted model vertices instead of 38,376. Animation channels use direct
sample indexing and reusable poses. Vertex transforms reuse vectors and transform
normals once per face. Smaller shadow footprint; rig preloaded during client setup.
VFX geometry stays below 10,000 vertices per effect and uses no particle clouds.
These are measured geometry/work limits, not a claim of measured in-game FPS.
The renderer/model wrappers are reused instead of rebuilding them every frame.
Movement uses a normal player collider, avoiding giant block scan volumes.
