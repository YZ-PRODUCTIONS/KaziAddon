param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-17.0.12/bin')
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/ea-crimson-tint-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
& "$JavaBin/javac.exe" -encoding UTF-8 -sourcepath "$repoPath/src/main/java" -d $classesPath "$PSScriptRoot/EaCrimsonTintTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Ea crimson tint test compilation failed.' }
& "$JavaBin/java.exe" -cp $classesPath EaCrimsonTintTest
if ($LASTEXITCODE -ne 0) { throw 'Ea crimson tint checks failed.' }
