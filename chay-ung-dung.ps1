. (Join-Path $PSScriptRoot 'cong-cu.ps1')
Set-Location -LiteralPath $PSScriptRoot
$tepChay = Join-Path $PSScriptRoot 'client\target\client-0.1.0.jar'
$tepGiaoThuc = Join-Path $PSScriptRoot 'giao-thuc\target\giao-thuc-0.1.0.jar'
if (!(Test-Path -LiteralPath $tepChay) -or !(Test-Path -LiteralPath $tepGiaoThuc)) {
    throw 'Chay .\bien-dich.ps1 truoc.'
}
$thuMucThuVien = Join-Path $PSScriptRoot 'client\target\phu-thuoc'
$duongDanMoDun = (Get-ChildItem -LiteralPath $thuMucThuVien -Filter 'javafx-*-win.jar' | ForEach-Object FullName) -join ';'
$cacThuVien = Get-ChildItem -LiteralPath $thuMucThuVien -Filter '*.jar' -File |
    Where-Object Name -NotLike 'giao-thuc-*.jar' | ForEach-Object FullName
$duongDanLop = (@($tepChay, $tepGiaoThuc) + $cacThuVien) -join ';'
& (Tim-Java) '-Dfile.encoding=UTF-8' '--module-path' $duongDanMoDun '--add-modules' 'javafx.controls' '-cp' $duongDanLop 'client.hatang.KhoiDong'
exit $LASTEXITCODE
