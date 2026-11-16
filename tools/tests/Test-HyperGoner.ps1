param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-17.0.12/bin')
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/goat-cutscene/tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
& "$JavaBin/javac.exe" -encoding UTF-8 -sourcepath "$repoPath/src/main/java" -d $classesPath "$PSScriptRoot/HyperGonerTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Hyper Goner test compilation failed.' }
$resourcesPath = Join-Path $repoPath 'src/main/resources'
$testClasspath = $classesPath + [IO.Path]::PathSeparator + $resourcesPath
& "$JavaBin/java.exe" -cp $testClasspath HyperGonerTest "$classesPath/preview.png"
if ($LASTEXITCODE -ne 0) { throw 'Hyper Goner checks failed.' }
