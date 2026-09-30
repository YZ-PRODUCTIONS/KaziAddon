# Kazi Additions Build Changelog — 2026-08-31

Changes made after the August 30, 2026 changelog. Temporary experiments that were later reverted are not included.

## Awakening

- Added fruit-specific global awakening announcements:
  - Koku: `(PlayerName) Has been unsealed...`
  - Kama: `(PlayerName) Has gained a new vessel...`
  - Gomu: `(PlayerName) Has reawakened the drums of liberation!`
  - Ope: `(PlayerName) Has opened K-Room and R-Room.`
  - Bomu: `(PlayerName) Now understands the true meaning of explosions!`
  - Kyoka: `(PlayerName): Shatter, Kyoka Suigetsu.`
  - Nagi: `(PlayerName): I must level up!`
  - Netsu: `(PlayerName): I stand at the pinnacle of all races.`
  - Kira: `(PlayerName): Shrine bright like a diamond`
  - Vampire: `(PlayerName) Has released control art system level 1..`
- Creative-mode players now bypass the one-hour Awakening Essence cooldown, and using an essence in Creative no longer starts that cooldown.
- Added login/respawn migration for the new Mochi awakening abilities so old Cart versions are replaced in unlocked and equipped slots.
- Cleaned null entries from Mine Mine no Mi's global Devil Fruit list after fruit injection, preventing a client startup crash during fruit color registration.

## Goro Goro no Mi

- Replaced Cart Addon's Goro versions with Mine Mine no Mi's base El Thor, Sango, and Vari.
- Removed Cart's Reworked El Thor, Reworked Sango, Reworked Vari, Reworked Raigo, Voltage Up, Volt Amaru, Volt Amaru Flight, and Shinzo Massage from Goro's move list.
- Restored the original El Thor and Sango behavior after the temporary terrain-damage nerfs were discarded.
- Corrected the displayed Goro move names, translations, and icons.

## Mochi Mochi no Mi

- Replaced Cart's Zan Giri Mochi, Kuri Mochi, and Mochi Ginchaku with locally controlled Kazi copies.
- Removed the Armament Haki activation requirement from all three cloned Mochi moves. Their attacks still retain their Hardening/Haki damage source.

### Zan Giri Mochi

- Added Zan Giri Mochi to Mochi's move list with its morph animation restored.
- Zan Giri's grab now bypasses Future Sight and Kami-e avoidance.
- Added client-side rendering safeguards to prevent the F5/layer crash.
- Zan Giri currently has a 1,200-tick cooldown.

### Kuri Mochi

- Removed the burst system. Kuri Mochi now fires continuously every 4 ticks for as long as it remains active.
- The main Kuri Mochi entity is independent of the user's facing direction, has its native AI disabled, and follows one block above the user's head.
- Shots prioritize the nearest non-allied player, then fall back to the nearest non-allied living entity.
- Projectile speed was increased by 50%, from 2.5 to 3.75.
- Cooldown now scales with active duration, up to the full 500-tick cooldown after the complete 200-tick duration.

### Mochi Ginchaku

- Donuts now spawn as a formation above the nearest non-allied player within 80 blocks. If no player is available, the nearest non-allied living entity is used.
- Every donut locks onto the same target rather than selecting targets individually.
- Projectiles fire automatically instead of following the user's mouse aim.
- Firing was slowed to one volley every 30 ticks.
- The formation was widened to a 15-block radius so the 15 donuts spawn farther apart.
- Corrected Mochi ability names, translations, and icons.

## Yami Yami no Mi

- Added a Cart `ReworkAbilities` mixin that replaces Cart's Yami lineup with the requested mostly-base move set:
  - Mine Mine no Mi Black Hole
  - Mine Mine no Mi Liberation
  - Mine Mine no Mi Black Road
  - Mine Mine no Mi Dark Matter
  - Mine Mine no Mi Kurouzu
  - Cart Addon Black Hand
  - Mine Mine no Mi Absorbed Blocks
- Removed Cart's reworked Yami moves and Yami Absorption Passive from the resulting fruit lineup.
- Dark Matter uses Mine Mine no Mi's original ability directly. The experimental held/M1 Dark Matter version was discarded.

## Other ability changes

- Doubled all Toki time-bar gains. Passive generation is now 2 points every 10 seconds, and other additions to the bar are doubled as well.
- Set Dismantle's normal/instant-use cooldown to 500 ticks.
- Unlimited Void now puts Koku's other techniques on 25% of the domain's cooldown instead of 50%, including domain-clash handling.
- Removed Sulong's cooldown reductions from Electrical Missile, Electrical Shower, Electrical Burst, and Electrical Tempesta. Their other Sulong bonuses remain active.

## Susu Susu no Mi

- Replaced Cart's Susu lineup with Kazi-owned copies of Obelisusu, Karasusu, Gokuro, Shokuro, Hijonna Kukuu, Rakuro, Susu Logia, Susu Fly, and Susu Immunity.
- Added custom implementations for Obelisusu and Gokuro while retaining Kazi-controlled copies of the remaining Cart behavior.
- Gokuro now uses a custom projectile and soot cloud that applies Poison III, Blindness III, and Soot III to affected targets.

## Stability and compatibility

- Removed the obsolete Kuri Mochi continuous-fire mixin; its continuous firing is handled entirely by the ability code.
- Added mixin gating so the Yami fruit replacement follows the existing `disableFruitChanges` configuration.
- Fixed multiple Mochi registration, rendering, and saved-ability migration problems encountered while replacing Cart's implementations.
