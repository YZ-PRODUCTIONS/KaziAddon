param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')

$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/inner-dragons-depth-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
$lwjglRoot = Join-Path $env:USERPROFILE '.gradle/caches/forge_gradle/maven_downloader/org/lwjgl'
$jars = @()
foreach ($module in @('lwjgl', 'lwjgl-glfw', 'lwjgl-opengl')) {
    foreach ($suffix in @('', '-natives-windows')) {
        $jarPath = Join-Path $lwjglRoot "$module/3.2.2/$module-3.2.2$suffix.jar"
        if (-not (Test-Path -LiteralPath $jarPath)) { throw "Missing local LWJGL dependency: $jarPath" }
        $jars += $jarPath
    }
}
$dependencies = $jars -join ';'
& "$JavaBin/javac.exe" -encoding UTF-8 -cp $dependencies -d $classesPath "$PSScriptRoot/InnerDragonsDepthTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Cinematic depth regression compilation failed.' }
# The test creates a GLFW_VISIBLE=false window. It never opens a game or a visible helper window.
& "$JavaBin/java.exe" -cp "$classesPath;$dependencies" InnerDragonsDepthTest
if ($LASTEXITCODE -ne 0) { throw 'Cinematic depth GPU regression failed.' }
