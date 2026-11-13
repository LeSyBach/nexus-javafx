. (Join-Path $PSScriptRoot 'cong-cu.ps1')
Set-Location -LiteralPath $PSScriptRoot
$tepChay = Join-Path $PSScriptRoot 'server\target\server-0.1.0.jar'
$tepGiaoThuc = Join-Path $PSScriptRoot 'giao-thuc\target\giao-thuc-0.1.0.jar'
if (!(Test-Path -LiteralPath $tepChay) -or !(Test-Path -LiteralPath $tepGiaoThuc)) {
    throw 'Chay .\bien-dich.ps1 truoc.'
}
$thuMucThuVien = Join-Path $PSScriptRoot 'server\target\phu-thuoc'
$cacThuVien = Get-ChildItem -LiteralPath $thuMucThuVien -Filter '*.jar' -File |
    Where-Object Name -NotLike 'giao-thuc-*.jar' | ForEach-Object FullName
$duongDanLop = (@($tepChay, $tepGiaoThuc) + $cacThuVien) -join ';'
& (Tim-Java) '-Dfile.encoding=UTF-8' '-cp' $duongDanLop 'server.hatang.MayChuChat' (Join-Path $PSScriptRoot 'cau-hinh\may-chu.properties')
exit $LASTEXITCODE
