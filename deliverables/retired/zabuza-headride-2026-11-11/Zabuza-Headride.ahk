#Requires AutoHotkey v2.0
#SingleInstance Force
#Warn All, StdOut

; Upfling reference: https://www.youtube.com/watch?v=L2Zs3Bf6Khc&t=7s
; Manual upfling and aim, followed by the jump/Water Clone keyboard inputs.
; The tutorial does not specify millisecond timings. Defaults are untested starting values.
SetKeyDelay(-1, -1)
GroupAdd("ZabuzaRoblox", "ahk_exe RobloxPlayerBeta.exe")
; Keep the old regular-ride preset separate from the upfling timing settings.
settingsFile := A_ScriptDir "\upfling-settings.ini"
state := {enabled: false, running: false, cancelled: false, target: 0, held: Map(),
    offset: ReadSetting("CloneOffsetMs", 50, -200, 200),
    startDelay: ReadSetting("StartDelayMs", 0, 0, 400),
    hold: ReadSetting("KeyHoldMs", 50, 20, 200)}

panel := Gui(, "Zabuza Upfling Headride")
panel.MarginX := 22
panel.MarginY := 18
panel.SetFont("s11", "Segoe UI")
panel.AddText("w440", "Land your upfling, aim toward the target, then tap F6.")
panel.AddText("w440", "Sends Space (jump) and 1 (Water Clone).`nYou perform the M1 upfling and control movement/aim.")
panel.AddText("w440 y+18", "Water Clone offset from jump (milliseconds)")
panel.AddEdit("vOffset w100", state.offset)
panel.AddUpDown("Range-200-200", state.offset)
panel.AddText("w440", "Positive: jump first. Negative: clone first. 0: together.")
panel.AddText("w440 y+14", "Wait after pressing F6 (milliseconds)")
panel.AddEdit("vStartDelay w100", state.startDelay)
panel.AddUpDown("Range0-400", state.startDelay)
panel.AddText("w440 y+14", "Key hold duration (milliseconds)")
panel.AddEdit("vHold w100", state.hold)
panel.AddUpDown("Range20-200", state.hold)
panel.AddText("w440 y+14", "Starting values: clone +50, wait 0, hold 50 ms.`nTune in training; the headride is not game-tested.")
panel.AddText("w440 y+12", "F6: after-upfling attempt    F8: pause / resume`nF9: settings in Roblox    F10: stop and pause anywhere`nCtrl+Alt+Esc: quit. Release Space/1 and close chat first.")
statusText := panel.AddText("w440 y+14 c555555", "Paused - enable when ready.")
panel.AddButton("w210 h34 y+12 Default", "Save and enable").OnEvent("Click", EnableMacro)
panel.AddButton("x+12 w210 h34", "Quit").OnEvent("Click", (*) => ExitApp())
panel.OnEvent("Close", (*) => ExitApp())
panel.OnEvent("Escape", (*) => ExitApp())
OnExit((*) => ReleaseKeys())
A_IconTip := "Zabuza Upfling Headride - paused"

; Checks parse, GUI creation and event ordering without showing UI or sending input.
if A_Args.Length && A_Args[1] = "--check" {
    CheckConfiguration()
    FileAppend("PASS: startup and upfling follow-up timing; no keyboard input sent.`n", "*")
    ExitApp()
}
panel.Show()

#HotIf IsRoblox() && state.enabled
*F6::RunHeadride()
#HotIf IsRoblox()
F8::ToggleMacro()
F9::ShowSettings()
#HotIf
F10::PauseMacro()
^!Esc::ExitApp()

CheckConfiguration() {
    for testCase in [
        {offset: -200, hold: 50, delay: 0},
        {offset: -60, hold: 200, delay: 400},
        {offset: 0, hold: 20, delay: 0},
        {offset: 50, hold: 50, delay: 0},
        {offset: 200, hold: 200, delay: 400}] {
        events := BuildEvents(testCase.offset, testCase.hold, testCase.delay)
        held := Map()
        presses := Map()
        previous := -1
        for event in events {
            if event.at < previous
                throw Error("Events are not chronological")
            previous := event.at
            for key in event.keys {
                if key != "Space" && key != "1"
                    throw Error("Upfling follow-up must only send jump and Water Clone")
                if event.down {
                    if held.Has(key)
                        throw Error("Duplicate key down")
                    held[key] := event.at
                    presses[key] := event.at
                } else {
                    if !held.Has(key) || event.at - held[key] != testCase.hold
                        throw Error("Incorrect key hold duration")
                    held.Delete(key)
                }
            }
        }
        if held.Count
            throw Error("Unreleased key")
        if presses.Count != 2 || presses["1"] - presses["Space"] != testCase.offset
            throw Error("Incorrect jump-to-clone offset")
        if Min(presses["1"], presses["Space"]) != testCase.delay
            throw Error("Incorrect delay after trigger")
    }
}

IsRoblox() {
    return WinActive("ahk_group ZabuzaRoblox")
}

ReadSetting(name, fallback, minimum, maximum) {
    global settingsFile
    try {
        value := IniRead(settingsFile, "Timing", name, fallback)
        if IsInteger(value) && value >= minimum && value <= maximum
            return Integer(value)
    }
    return fallback
}

EnableMacro(*) {
    global panel, state, settingsFile
    values := panel.Submit(false)
    if !IsInteger(values.Offset) || values.Offset < -200 || values.Offset > 200
        return MsgBox("Clone offset must be between -200 and 200.")
    if !IsInteger(values.Hold) || values.Hold < 20 || values.Hold > 200
        return MsgBox("Key hold must be between 20 and 200.")
    if !IsInteger(values.StartDelay) || values.StartDelay < 0 || values.StartDelay > 400
        return MsgBox("Wait after F6 must be between 0 and 400.")
    try {
        IniWrite(values.Offset, settingsFile, "Timing", "CloneOffsetMs")
        IniWrite(values.Hold, settingsFile, "Timing", "KeyHoldMs")
        IniWrite(values.StartDelay, settingsFile, "Timing", "StartDelayMs")
    } catch as err {
        return MsgBox("Could not save settings: " err.Message)
    }
    state.offset := Integer(values.Offset)
    state.hold := Integer(values.Hold)
    state.startDelay := Integer(values.StartDelay)
    state.enabled := true
    A_IconTip := "Zabuza Upfling Headride - ready (F10 pauses)"
    panel.Hide()
    Notice("Ready - land your upfling, aim, then tap F6.")
}

PauseMacro(*) {
    global state, statusText
    state.enabled := false
    state.cancelled := true
    ReleaseKeys()
    statusText.Text := "Paused - enable when ready."
    A_IconTip := "Zabuza Upfling Headride - paused"
    Notice("Zabuza macro paused.")
}

ToggleMacro(*) {
    global state
    if state.enabled
        PauseMacro()
    else {
        state.enabled := true
        A_IconTip := "Zabuza Upfling Headride - ready (F10 pauses)"
        Notice("Zabuza macro ready.")
    }
}

ShowSettings(*) {
    global panel
    PauseMacro()
    panel.Show()
}

BuildEvents(offset, hold, startDelay := 0) {
    jumpTime := startDelay + Max(0, -offset)
    cloneTime := startDelay + Max(0, offset)
    unsorted := [
        {at: jumpTime, keys: ["Space"], down: true},
        {at: jumpTime + hold, keys: ["Space"], down: false},
        {at: cloneTime, keys: ["1"], down: true},
        {at: cloneTime + hold, keys: ["1"], down: false}]
    events := []
    for event in unsorted {
        position := 1
        while position <= events.Length && events[position].at <= event.at
            position += 1
        events.InsertAt(position, event)
    }
    return events
}

CanContinue() {
    global state
    return state.enabled && !state.cancelled && WinActive("ahk_id " state.target)
}

RunHeadride() {
    global state
    if state.running || !state.enabled || !IsRoblox()
        return
    ; The upfling/M1 setup and all movement/aiming remain under user control.
    for key in ["Space", "1", "Ctrl", "Alt", "LWin", "RWin"] {
        if GetKeyState(key, "P") {
            Notice("Release Space, 1, Ctrl, Alt and Win before F6.")
            KeyWait("F6")
            return
        }
    }
    state.running := true
    state.cancelled := false
    state.target := WinExist("A")
    timerSet := false
    try {
        timerSet := DllCall("Winmm\timeBeginPeriod", "UInt", 1) = 0
        events := BuildEvents(state.offset, state.hold, state.startDelay)
        started := DllCall("GetTickCount64", "UInt64")
        for event in events {
            while DllCall("GetTickCount64", "UInt64") - started < event.at {
                if !CanContinue()
                    return
                Sleep(1)
            }
            Critical("On")
            try {
                if !CanContinue()
                    return
                sequence := "{Blind}"
                for key in event.keys {
                    if event.down {
                        state.held[key] := true
                        sequence .= "{" key " down}"
                    } else {
                        if state.held.Has(key)
                            state.held.Delete(key)
                        if !GetKeyState(key, "P")
                            sequence .= "{" key " up}"
                    }
                }
                SendEvent(sequence)
            } finally {
                Critical("Off")
            }
        }
    } finally {
        ReleaseKeys()
        if timerSet
            DllCall("Winmm\timeEndPeriod", "UInt", 1)
        state.running := false
        ; Prevent key repeat from starting another attempt while F6 stays held.
        KeyWait("F6")
    }
}

ReleaseKeys() {
    global state
    Critical("On")
    try {
        sequence := "{Blind}"
        for key in state.held {
            if !GetKeyState(key, "P")
                sequence .= "{" key " up}"
        }
        state.held.Clear()
        if sequence != "{Blind}"
            SendEvent(sequence)
    } finally {
        Critical("Off")
    }
}

Notice(message) {
    ToolTip(message)
    SetTimer(() => ToolTip(), -1800)
}
