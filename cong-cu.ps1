$ErrorActionPreference = 'Stop'
function Tim-Java {
    $cacNoi = @()
    if ($env:JAVA_HOME) { $cacNoi += (Join-Path $env:JAVA_HOME 'bin\java.exe') }
    $cacNoi += 'C:\Program Files\Java\jdk-17\bin\java.exe'
    $lenh = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($lenh) { $cacNoi += $lenh.Source }
    foreach ($noi in $cacNoi) { if (Test-Path -LiteralPath $noi) { return $noi } }
    throw 'Chua tim thay JDK 17. Dien JAVA_HOME roi chay lai.'
}
function Tim-Maven {
    $lenh = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($lenh) { return $lenh.Source }
    foreach ($noi in @('D:\IntelliJ IDEA 2024.2.1\plugins\maven\lib\maven3\bin\mvn.cmd','D:\IntelliJ IDEA Community Edition 2024.2.1\plugins\maven\lib\maven3\bin\mvn.cmd')) {
        if (Test-Path -LiteralPath $noi) { return $noi }
    }
    foreach ($thuMucGoc in @('C:\Program Files\JetBrains', (Join-Path $env:LOCALAPPDATA 'Programs'))) {
        if (!(Test-Path -LiteralPath $thuMucGoc)) { continue }
        foreach ($thuMuc in (Get-ChildItem -LiteralPath $thuMucGoc -Directory -Filter '*IntelliJ*' | Sort-Object Name -Descending)) {
            $tepMaven = Join-Path $thuMuc.FullName 'plugins\maven\lib\maven3\bin\mvn.cmd'
            if (Test-Path -LiteralPath $tepMaven) { return $tepMaven }
        }
    }
    throw 'Chua tim thay Maven. Cai Maven hoac mo pom.xml trong IntelliJ de build.'
}
