#Requires AutoHotkey v2.0
#SingleInstance Force
#Warn All, StdOut

; Technique: https://www.youtube.com/watch?v=fDDWG627Xas
; Hit with the rear knife hitbox, then face the target for the finisher/throw.
; Mouse distance and timing must be calibrated in-game; these defaults are estimates.
SetKeyDelay(-1, -1)
GroupAdd("KnifeRoblox", "ahk_exe RobloxPlayerBeta.exe")
settingsFile := A_ScriptDir "\settings.ini"
config := {turnCounts: ReadSetting("TurnCounts", 1200, 50, 20000),
    returnDelay: ReadSetting("ReturnDelayMs", 1000, 100, 3000),
    turnMs: ReadSetting("TurnDurationMs", 20, 0, 100),
    settleMs: ReadSetting("SettleMs", 40, 0, 200)}
state := {enabled: false, running: false, cancelled: false, target: 0, knifeHeld: false}

panel := Gui(, "Kiritsugu Combat Knife Extend")
panel.MarginX := 22
panel.MarginY := 16
panel.SetFont("s11", "Segoe UI")
panel.AddText("w460", "Stand close, face the target, and enable shift lock.")
panel.AddText("w460", "F6 turns away, uses Combat Knife (3), then turns back.`nYou handle movement and the follow-up M1s.")
panel.AddText("w460 y+16", "Half-turn mouse distance (relative mouse counts)")
panel.AddEdit("vTurnCounts w110", config.turnCounts)
; Keep the edit's submitted text numeric (Windows otherwise inserts commas).
panel.AddUpDown("Range50-20000 0x80", config.turnCounts)
panel.AddText("w460", "Save, then use F7 in Roblox to test a 180-degree turn.`nIncrease if it turns too little; decrease if it turns too far.")
panel.AddText("w460 y+12", "Delay from Combat Knife to turn back (milliseconds)")
panel.AddEdit("vReturnDelay w110", config.returnDelay)
panel.AddUpDown("Range100-3000 0x80", config.returnDelay)
panel.AddText("w460", "Tune this to the finisher flash, starting without Triple Accel.")
panel.AddText("w220 y+12", "Turn duration (ms)")
panel.AddEdit("vTurnMs w110", config.turnMs)
panel.AddUpDown("Range0-100 0x80", config.turnMs)
panel.AddText("w460 y+12", "Wait after turning away, before knife (ms)")
panel.AddEdit("vSettleMs w110", config.settleMs)
panel.AddUpDown("Range0-200 0x80", config.settleMs)
panel.AddText("w460 y+14", "1200 counts / 1000 ms are untested starting values.`nCalibrate your turn first, then tune the knife delay.")
panel.AddText("w460 y+12", "F6: extend attempt    F7: test turn    Shift+F7: reverse test`nF8: pause/resume    F9: settings    F10: stop anywhere`nCtrl+Alt+Esc: quit. Close chat and menus before testing.")
statusText := panel.AddText("w460 y+12 c555555", "Paused. Save your settings, then calibrate with F7.")
panel.AddButton("w220 h34 y+10 Default", "Save and enable").OnEvent("Click", SaveAndEnable)
panel.AddButton("x+12 w220 h34", "Quit").OnEvent("Click", (*) => ExitApp())
panel.OnEvent("Close", (*) => ExitApp())
panel.OnEvent("Escape", (*) => ExitApp())
OnExit((*) => ReleaseKnife())
A_IconTip := "Kiritsugu knife extend - paused"

if A_Args.Length && A_Args[1] = "--check" {
    CheckPlans()
    FileAppend("PASS: settings form, startup, turn calibration totals, event order, balanced knife inputs and cancellation guards. No input sent.`n", "*")
    ExitApp()
}
panel.Show()

#HotIf IsRoblox() && state.enabled
*F6::RunAction("extend", "F6")
#HotIf IsRoblox()
F7::RunAction("test-right", "F7")
+F7::RunAction("test-left", "F7")
F8::ToggleMacro()
F9::ShowSettings()
#HotIf
F10::PauseMacro()
^!Esc::ExitApp()

IsRoblox() {
    return WinActive("ahk_group KnifeRoblox")
}

ReadSetting(name, fallback, minimum, maximum) {
    global settingsFile
    try {
        value := IniRead(settingsFile, "KnifeExtend", name, fallback)
        if IsInteger(value) && value >= minimum && value <= maximum
            return Integer(value)
    }
    return fallback
}

SaveAndEnable(*) {
    global panel, config, state, settingsFile
    values := panel.Submit(false)
    for field in [
        {name: "TurnCounts", low: 50, high: 20000},
        {name: "ReturnDelay", low: 100, high: 3000},
        {name: "TurnMs", low: 0, high: 100},
        {name: "SettleMs", low: 0, high: 200}] {
        value := values.%field.name%
        if !IsInteger(value) || value < field.low || value > field.high
            return MsgBox(field.name " must be a whole number from " field.low " to " field.high ".")
    }
    try {
        IniWrite(values.TurnCounts, settingsFile, "KnifeExtend", "TurnCounts")
        IniWrite(values.ReturnDelay, settingsFile, "KnifeExtend", "ReturnDelayMs")
        IniWrite(values.TurnMs, settingsFile, "KnifeExtend", "TurnDurationMs")
        IniWrite(values.SettleMs, settingsFile, "KnifeExtend", "SettleMs")
    } catch as err {
        return MsgBox("Could not save settings: " err.Message)
    }
    config := {turnCounts: Integer(values.TurnCounts), returnDelay: Integer(values.ReturnDelay),
        turnMs: Integer(values.TurnMs), settleMs: Integer(values.SettleMs)}
    state.enabled := true
    A_IconTip := "Kiritsugu knife extend - ready"
    panel.Hide()
    Notice("Ready. F7 tests the turn; F6 attempts the knife extend.")
}

PauseMacro(*) {
    global state, statusText
    state.enabled := false
    state.cancelled := true
    ReleaseKnife()
    A_IconTip := "Kiritsugu knife extend - paused"
    statusText.Text := "Paused. F7 can still test the saved turn distance in Roblox."
    Notice("Knife extend stopped and paused.")
}

ToggleMacro(*) {
    global state
    if state.enabled
        PauseMacro()
    else {
        state.enabled := true
        A_IconTip := "Kiritsugu knife extend - ready"
        Notice("Knife extend ready.")
    }
}

ShowSettings(*) {
    global panel
    PauseMacro()
    panel.Show()
}

BuildTurnPlan(counts, startAt, duration) {
    plan := []
    steps := Max(1, Ceil(duration / 4))
    moved := 0
    Loop steps {
        total := Round(counts * A_Index / steps)
        plan.Push({at: startAt + Round((A_Index - 1) * duration / steps),
            kind: "move", dx: total - moved})
        moved := total
    }
    return plan
}

BuildKnifePlan(options) {
    plan := BuildTurnPlan(options.turnCounts, 0, options.turnMs)
    knifeAt := options.turnMs + options.settleMs
    plan.Push({at: knifeAt, kind: "knife-down"})
    plan.Push({at: knifeAt + 40, kind: "knife-up"})
    for event in BuildTurnPlan(-options.turnCounts, knifeAt + options.returnDelay, options.turnMs)
        plan.Push(event)
    return plan
}

CanContinue(requireEnabled) {
    global state
    return !state.cancelled && (!requireEnabled || state.enabled)
        && state.target && WinActive("ahk_id " state.target)
}

NowMs() {
    static frequency := ClockFrequency()
    counter := 0
    DllCall("QueryPerformanceCounter", "Int64*", &counter)
    return counter * 1000.0 / frequency
}

ClockFrequency() {
    value := 0
    if !DllCall("QueryPerformanceFrequency", "Int64*", &value) || value <= 0
        throw Error("Could not initialize the timing clock")
    return value
}

RunAction(action, triggerKey) {
    global state, config
    requireEnabled := action = "extend"
    if state.running || !IsRoblox() || (requireEnabled && !state.enabled)
        return
    for key in ["3", "Ctrl", "Alt", "LWin", "RWin"] {
        if GetKeyState(key, "P") {
            Notice("Release 3, Ctrl, Alt and Win before using the macro.")
            KeyWait(triggerKey)
            return
        }
    }
    state.running := true
    state.cancelled := false
    state.target := WinExist("A")
    timerSet := false
    try {
        plan := requireEnabled ? BuildKnifePlan(config)
            : BuildTurnPlan(config.turnCounts * (action = "test-left" ? -1 : 1), 0, config.turnMs)
        timerSet := DllCall("Winmm\timeBeginPeriod", "UInt", 1) = 0
        started := NowMs()
        for event in plan {
            while NowMs() - started < event.at {
                if !CanContinue(requireEnabled)
                    return
                Sleep(1)
            }
            Critical("On")
            try {
                if !CanContinue(requireEnabled)
                    return
                if event.kind = "move" {
                    ; Relative mouse input, including when Roblox locks the cursor.
                    ; This does not read or write game memory.
                    DllCall("user32\mouse_event", "UInt", 0x0001,
                        "Int", event.dx, "Int", 0, "UInt", 0, "UPtr", 0)
                } else if event.kind = "knife-down" {
                    state.knifeHeld := true
                    SendEvent("{Blind}{3 down}")
                } else if event.kind = "knife-up" {
                    ReleaseKnife()
                }
            } finally {
                Critical("Off")
            }
        }
    } catch as err {
        PauseMacro()
        Notice("Macro paused: " err.Message)
    } finally {
        ReleaseKnife()
        if timerSet
            DllCall("Winmm\timeEndPeriod", "UInt", 1)
        state.running := false
        ; One sequence per press, even if the trigger stays held down.
        KeyWait(triggerKey)
    }
}

ReleaseKnife() {
    global state
    Critical("On")
    try {
        if state.knifeHeld {
            if !GetKeyState("3", "P")
                SendEvent("{Blind}{3 up}")
            state.knifeHeld := false
        }
    } finally {
        Critical("Off")
    }
}

Notice(message) {
    ToolTip(message)
    SetTimer(() => ToolTip(), -1800)
}

CheckPlans() {
    global state, panel, config
    ; Exercise the actual form values so formatting cannot break Save and enable.
    formValues := panel.Submit(false)
    for field in [
        {name: "TurnCounts", expected: config.turnCounts},
        {name: "ReturnDelay", expected: config.returnDelay},
        {name: "TurnMs", expected: config.turnMs},
        {name: "SettleMs", expected: config.settleMs}] {
        value := formValues.%field.name%
        if !IsInteger(value) || Integer(value) != field.expected
            throw Error("Settings form did not return a valid " field.name)
    }
    for options in [
        {turnCounts: 50, returnDelay: 100, turnMs: 0, settleMs: 0},
        {turnCounts: 1200, returnDelay: 1000, turnMs: 20, settleMs: 40},
        {turnCounts: 1637, returnDelay: 400, turnMs: 17, settleMs: 60},
        {turnCounts: 20000, returnDelay: 3000, turnMs: 100, settleMs: 200}] {
        plan := BuildKnifePlan(options)
        previous := -1
        moved := 0
        held := false
        pressAt := -1
        presses := 0
        for event in plan {
            if event.at < previous
                throw Error("Events are not chronological")
            previous := event.at
            if event.kind = "move" {
                moved += event.dx
            } else if event.kind = "knife-down" {
                if held || moved != options.turnCounts
                    throw Error("Knife must start after the turn away")
                pressAt := event.at
                held := true
                presses += 1
            } else if event.kind = "knife-up" {
                if !held || event.at - pressAt != 40
                    throw Error("Incorrect knife hold")
                held := false
            } else {
                throw Error("Unexpected input")
            }
        }
        if held || presses != 1 || moved != 0
            throw Error("Unbalanced knife/turn sequence")
        expectedReturnAt := options.turnMs + options.settleMs + options.returnDelay
        for event in plan {
            if event.kind = "move" && event.dx < 0 && event.at < expectedReturnAt
                throw Error("Return turn occurs too soon")
        }
        for sign in [-1, 1] {
            total := 0
            for event in BuildTurnPlan(options.turnCounts * sign, 0, options.turnMs)
                total += event.dx
            if total != options.turnCounts * sign
                throw Error("Calibration turn does not match requested distance")
        }
    }
    if NowMs() <= 0
        throw Error("Invalid timing clock")
    state.cancelled := true
    if CanContinue(false) || CanContinue(true)
        throw Error("Cancelled input may continue")
    state.cancelled := false
    state.enabled := false
    if CanContinue(true)
        throw Error("Paused macro may continue")
    state.target := 0
    if CanContinue(false)
        throw Error("Input allowed without a target")
}
