param([switch]$BoQuaKiemTra)
. (Join-Path $PSScriptRoot 'cong-cu.ps1')
Set-Location -LiteralPath $PSScriptRoot
$lenhJava = Tim-Java
$env:JAVA_HOME = Split-Path (Split-Path $lenhJava -Parent) -Parent
$cacThamSo = @('-B', ('-Dmaven.repo.local=' + (Join-Path $PSScriptRoot '.cong-cu\kho-maven')), 'install')
if ($BoQuaKiemTra) { $cacThamSo += '-DskipTests' }
& (Tim-Maven) @cacThamSo
exit $LASTEXITCODE
