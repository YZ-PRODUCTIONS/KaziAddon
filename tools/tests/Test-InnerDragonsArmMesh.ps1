param([string]$JavaBin = 'C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
$ErrorActionPreference = 'Stop'
$repoPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$classesPath = Join-Path $repoPath 'output/validation/inner-dragons-arm-mesh-tests'
[IO.Directory]::CreateDirectory($classesPath) | Out-Null
& "$JavaBin/javac.exe" -encoding UTF-8 -d $classesPath `
    "$repoPath/src/main/java/net/kazi/kazimod/client/nagi/InnerDragonsArmMesh.java" `
    "$PSScriptRoot/InnerDragonsArmMeshTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Inner Dragons arm mesh test compilation failed.' }
& "$JavaBin/java.exe" -cp $classesPath InnerDragonsArmMeshTest
if ($LASTEXITCODE -ne 0) { throw 'Inner Dragons arm mesh checks failed.' }
