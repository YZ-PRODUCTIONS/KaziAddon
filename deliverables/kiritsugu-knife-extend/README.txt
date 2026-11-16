KIRITSUGU COMBAT KNIFE EXTEND
Windows / AutoHotkey v2

Open Start Kiritsugu Knife Extend.lnk and click Save and enable.

CALIBRATE FIRST
1. Enter ABA training as base-form Kiritsugu, without Triple Accel active.
2. Enable shift lock yourself and close chat/menus.
3. Face a clear landmark and press F7. Adjust the mouse distance in F9 settings
   until F7 turns exactly halfway around (180 degrees). Shift+F7 tests the reverse.
4. Save again after each change. A larger number turns farther. Keep your Roblox
   sensitivity and camera mode consistent after calibration.

USE THE EXTEND
Stand very close to the opponent and face them, with Combat Knife ready.
Tap F6. The macro turns away, presses 3, waits, then turns back.
The initial turn positions the opponent behind you so the rear knife hitbox hits.
The return turn should line up with the finisher flash, directing the backflip
and thrown knife toward the target. Continue your M1 combo manually.
Do not move your mouse during the two automated turns.

TIMING
The 1200-count half-turn and 1000 ms knife delay are estimates, not measured or
verified settings. Calibrate the turn distance before adjusting attack timing.
Try changing the knife-to-return delay in 25 ms steps. Smaller = turn back sooner.
The delay is measured from the 3 key press to the start of the return turn.
Turn duration controls how the mouse movement is spread out (default 20 ms).
The settle delay comes after turning away and before pressing 3 (default 40 ms).
The 3 key is held for 40 ms. Changes save in settings.ini next to the script.

Triple Accel changes Combat Knife's speed. This preset starts with an ordinary
base-form knife attack; it neither activates nor detects Accel or awakening.
If you want to use Accel, tune the delay separately under that condition.

CONTROLS
F6               One knife extend attempt, while enabled and Roblox is active
F7               Test a right half-turn in Roblox, even while paused
Shift + F7       Test a left half-turn in Roblox, even while paused
F8               Pause / resume in Roblox
F9               Pause and open settings in Roblox
F10              Cancel the current sequence and pause, from any app
Ctrl + Alt + Esc Quit, from any app

LIMITS
You control positioning, shift lock, movement, follow-up M1s and cooldowns.
The macro sends only relative horizontal mouse movement and the 3 key.
It does not track an opponent, inspect pixels/game memory, or detect the finisher.
It does not toggle shift lock or change your sensitivity.
It runs only when RobloxPlayerBeta.exe is the active window.
Switching windows or pressing F10 cancels pending input and releases the macro's
knife key. An interrupted turn is not automatically reversed in another app.
Holding F6/F7 runs only one sequence until the key is released.
Keep chat and menus closed whenever you use its hotkeys.

VERIFICATION
The script's startup, input schedule, relative turn totals and cancellation guards
were checked without sending game input. Successful ABA extends have not been
tested. Game updates, frame rate, latency, camera mode and input handling may
require changes to these settings. Net-zero mouse counts do not guarantee an
identical view when mouse acceleration, clamping or manual movement intervenes.

TECHNIQUE REFERENCE
BunglesG - Guide for Kiritsugu's Combat knife extend tech
https://www.youtube.com/watch?v=fDDWG627Xas
The opening explains hitting with the rear hitbox; around 0:13 the guide shows
when to face the target again. Around 0:16 it explains the throw/backflip follow-up.
The guide provides visual cues, not exact macro delays or mouse distances.

Run the local no-input check with:
AutoHotkey64.exe /ErrorStdOut Kiritsugu-Knife-Extend.ahk --check
