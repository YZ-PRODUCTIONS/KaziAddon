param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')

# Compile only the actual progression class against small capability fixtures.
# The final wiring checks read already-compiled production classes; no Gradle or jar build.
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/yagi-progression-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
$fixturesPath = Join-Path $PSScriptRoot 'yagi_progression_fixtures'
$fixtureSources = @(Get-ChildItem -LiteralPath $fixturesPath -Filter '*.java' -Recurse -File | ForEach-Object { $_.FullName })
$progressionPath = Join-Path $repoPath 'src/main/java/net/kazi/kazimod/abilities/YagiAwakening/YagiAwakeningProgression.java'
& "$JavaBin/javac.exe" -encoding UTF-8 -d $classesPath $fixtureSources $progressionPath "$PSScriptRoot/YagiAwakeningProgressionTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Yagi progression harness compilation failed.' }
& "$JavaBin/java.exe" -cp $classesPath YagiAwakeningProgressionTest
if ($LASTEXITCODE -ne 0) { throw 'Yagi progression lifecycle checks failed.' }

$productionClasses = Join-Path $repoPath 'build/classes/java/main'
$checks = 0
function Read-Bytecode([string]$ClassName) {
    $result = (& "$JavaBin/javap.exe" -classpath $productionClasses -p -c $ClassName) -join "`n"
    if ($LASTEXITCODE -ne 0) { throw "Compile $ClassName before running the wiring check." }
    return $result
}
function Read-Method([string]$Code, [string]$Name) {
    $pattern = '(?ms)^  [^\r\n]*\b' + [regex]::Escape($Name) + '\([^\r\n]*\);\r?\n    Code:\r?\n(?<Body>.*?)(?=^  (?:public|private|protected|static)|^})'
    $body = [regex]::Match($Code, $pattern).Groups['Body'].Value
    if ([string]::IsNullOrEmpty($body)) { throw "Compiled method $Name not found." }
    return $body
}
function Assert-Wiring([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
    $script:checks++
}
$lifecycle = Read-Bytecode 'net.kazi.kazimod.events.AwakeningAbilityLoginFix'
$unlock = Read-Method $lifecycle 'onUnlockAbility'
$deny = [regex]::Match($unlock, '(?s)KaziConfig.INSTANCE:.*?YagiAwakeningProgression.isRetired:.*?YagiAwakeningSupport.canUnlock:.*?Event\$Result.DENY:.*?UnlockAbilityEvent.setResult:.*?return').Value
Assert-Wiring (-not [string]::IsNullOrEmpty($deny)) 'The central unlock event must deny only configured, retired moves belonging to awakened Goat.'
Assert-Wiring ($deny.Contains('disableFruitChanges:') -and $deny.Contains('Boolean.booleanValue:')) 'The retirement denial must respect the fruit-change configuration.'
Assert-Wiring ($unlock.IndexOf('hasAwakenedFruit:') -ge 0 -and $unlock.IndexOf('hasAwakenedFruit:') -lt $unlock.IndexOf('YagiAwakeningProgression.isRetired:')) 'Unawakened fruit owners must bypass the retirement denial.'
Assert-Wiring ($lifecycle.Contains('YagiAwakeningProgression.sync:')) 'The existing login lifecycle must continue migrating awakened Goat state.'
$support = Read-Method (Read-Bytecode 'net.kazi.kazimod.abilities.YagiAwakening.YagiAwakeningSupport') 'canUnlock'
foreach ($required in @('hasAwakenedFruit:', 'hasDevilFruit:', 'KaziItems.YAGI_YAGI_NO_MI_MODEL_MOUNTAIN_GOAT:')) {
    Assert-Wiring ($support.Contains($required)) "Real awakening eligibility requires $required"
}
$flight = Read-Method (Read-Bytecode 'net.kazi.kazimod.abilities.Yagi.AstralFlightProgression') 'sync'
Assert-Wiring ($flight.Contains('AbilityHelper.checkAndUnlockAbility:')) 'Astral Flight must retain its native unlock route so eligible owners can receive it on login.'
Assert-Wiring (-not $flight.Contains('addUnlockedAbility:') -and -not $flight.Contains('addPassiveAbility:')) 'Astral Flight must not bypass unlock events by granting itself directly.'
Write-Output "Yagi retirement wiring: $checks compiled-code checks passed."
