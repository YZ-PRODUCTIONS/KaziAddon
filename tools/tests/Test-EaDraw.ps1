param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-17.0.12/bin')
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/ea-draw-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
& "$JavaBin/javac.exe" -encoding UTF-8 -sourcepath "$repoPath/src/main/java" -d $classesPath "$PSScriptRoot/EaDrawTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Ea draw test compilation failed.' }
& "$JavaBin/java.exe" -cp $classesPath EaDrawTest "$classesPath/preview.png"
if ($LASTEXITCODE -ne 0) { throw 'Ea draw checks failed.' }
