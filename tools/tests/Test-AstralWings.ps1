param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-17.0.12/bin')
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/astral-wings/tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
& "$JavaBin/javac.exe" -encoding UTF-8 -sourcepath "$repoPath/src/main/java" -d $classesPath "$PSScriptRoot/AstralWingsTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Astral wings test compilation failed.' }
& "$JavaBin/java.exe" -cp $classesPath AstralWingsTest "$classesPath/preview.png"
if ($LASTEXITCODE -ne 0) { throw 'Astral wings checks failed.' }
