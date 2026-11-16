ZABUZA UPFLING HEADRIDE - WINDOWS / AUTOHOTKEY v2

This revision handles the jump/Water Clone follow-up to a MANUAL UPFLING.
The old regular clone-ride dash input has been removed.

1. Quit any running old copy with Ctrl+Alt+Esc, then reopen
   Start Zabuza Headride.lnk (the same launcher now opens this revision).
2. Click Save and enable. Starting values: clone offset +50 ms,
   wait after F6 0 ms, key hold 50 ms. These are UNTESTED tuning values.
3. In ABA training, select base-form Zabuza and close chat and menus.
4. Perform your M1 upfling manually. Aim up toward the launched opponent,
   and release Space and 1 if you were holding them.
5. Tap F6 as the upfling finishes. It sends a jump and Water Clone with the
   configured offset to attempt the airborne clone ride. Position yourself
   to land on the clone; movement and aiming are yours to control.
6. Continue the combo yourself. F6 does not perform M1s or follow-up attacks.

F6 DOES NOT START THE FULL COMBO FROM NEUTRAL.
It automates the keyboard follow-up after you have launched your opponent.
It does not detect a hit, target height, cooldowns or whether a ride succeeds.

CONTROLS
F6               One after-upfling attempt (while Roblox is active)
F8               Pause / resume (in Roblox)
F9               Pause and reopen settings (in Roblox)
F10              Stop the current attempt and pause (works anywhere)
Ctrl + Alt + Esc Quit (works anywhere)

TIMING
Water Clone offset is measured from the jump key press:
  Positive (default +50 ms): jump first, then cast Water Clone.
  0: jump and clone together.
  Negative: clone first, then jump.
Try changing the offset by 10 ms per trial. A grounded clone can mean the
cast was too early or the aim/position was wrong; timing alone cannot aim it.

Wait after F6 shifts BOTH inputs later by the same amount (0-400 ms).
Use it if you prefer pressing F6 during the end of the upfling animation.
Key hold controls how long each key is held (20-200 ms; default 50 ms).
Each key's hold duration is independent of the other timing settings.

Settings save in upfling-settings.ini. Old settings.ini values from the
regular clone-ride version are deliberately not loaded.

LIMITS AND VERIFICATION
Uses keyboard input only: Space and 1. You handle M1s, camera aiming and movement.
Runs in the standard desktop Roblox player (RobloxPlayerBeta.exe).
Changing away from the game cancels pending input and releases macro-held keys.
Holding F6 does not repeatedly run the sequence.

References:
Milky - Zabuza Water Clone Upfling Extend 100-0 Tutorial, around 0:07-0:10:
https://www.youtube.com/watch?v=L2Zs3Bf6Khc&t=7s
Subtilizer - upfling extension discussion, around 1:16-1:50:
https://www.youtube.com/watch?v=hw0fSafvz_c&t=76s

The demonstrations show the technique but do not specify exact keyboard timings.
The +50 ms offset is an implementation starting point, not a measured game value.
Startup, event order, key hold durations and permitted output keys are checked.
Successful headrides have NOT been tested in ABA. The current game, frame rate,
ping, trigger timing and camera/position can affect whether this works.

No game memory reading, injection, downloads or network access is used.
To check startup and event ordering without sending keys:
AutoHotkey64.exe /ErrorStdOut Zabuza-Headride.ahk --check
