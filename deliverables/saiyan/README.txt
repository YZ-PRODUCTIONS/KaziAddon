SAIYAN BLOCKBENCH MODELS

Open the .bbmodel files directly in Blockbench. Textures are embedded.
All six models use cuboids and include editable animation clips.

super_saiyan_hair.bbmodel       43 cubes; idle and flight clips
super_saiyan_2_hair.bbmodel     40 cubes; idle and flight clips
super_saiyan_3_hair.bbmodel    106 cubes; articulated long locks
super_saiyan_god_hair.bbmodel   43 cubes; shorter red style
super_saiyan_blue_hair.bbmodel  43 cubes; blue spiked style
dragon_fist.bbmodel           145 cubes; surge and jaw/roar clips

Hair fits an 8x8x8 player head at Y=0..8. Front is positive Z.
The preview head is only a fitting stand-in and is not part of the files.
In-game hair follows the player's head and adds damped, movement-driven sway.
The dragon has an animated body and jaw, plus God/Blue texture variants in
the mod resources. The rendered dragon uses full brightness.

The mod uses baked geometry exported from these exact models.
Runtime geometry: assets/kazimod/models/saiyan/*.json
Runtime textures: assets/kazimod/textures/models/saiyan/*.png
Model authoring/export script: work/saiyan_models_meteor/build-models.cjs

Model previews were rendered separately from Minecraft. In-game movement,
PvP grabs, and compatibility with other player-rendering mods still need
a gameplay check.
