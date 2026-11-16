param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-17.0.12/bin')
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/asriel-flow-pattern-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
& "$JavaBin/javac.exe" -encoding UTF-8 -sourcepath "$repoPath/src/main/java" -d $classesPath "$PSScriptRoot/AsrielFlowPatternTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Flowing ink checks failed to compile.' }
& "$JavaBin/java.exe" -cp $classesPath AsrielFlowPatternTest
if ($LASTEXITCODE -ne 0) { throw 'Flowing ink regression failed.' }
