# inject-song.ps1 - 把 MP3 打进 Lapis-Zgrnf mod jar
#
# Lapis-Zgrnf mod 不再内置音乐(避免版权问题),需要自己准备 MP3。
# 用法:
#   1. 把本脚本、Lapis-Zgrnf 的 mod jar(Lapis-Zgrnf-fabric-*.jar / Lapis-Zgrnf-neoforge-*.jar)
#      和一个 .mp3 放到同一个文件夹
#   2. 在 PowerShell 里运行:  .\inject-song.ps1
#
# 脚本会自动:
#   - 找出目录里的第一个 .mp3(或用 -Mp3 指定)
#   - 找出所有 Lapis-Zgrnf-fabric-*.jar / Lapis-Zgrnf-neoforge-*.jar(或用 -Jar 指定)
#   - 把 mp3 写入每个 jar 的 assets/zgrnf/song.mp3(mod 从这个路径读取播放)
#
# 参数示例:
#   .\inject-song.ps1 -Mp3 "C:\Users\me\Desktop\my-song.mp3" -Jar "C:\mods\Lapis-Zgrnf-fabric-26.2-26.2.0.0.jar"
#   .\inject-song.ps1 -Mp3 "song.mp3" -Jar Lapis-Zgrnf-fabric-*.jar,Lapis-Zgrnf-neoforge-*.jar

param(
    [string]$Mp3 = '',
    [string[]]$Jar = @()
)

$ErrorActionPreference = 'Stop'

# 让中文提示在 PowerShell 控制台正确显示
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }

$dir = if ($PSScriptRoot) { $PSScriptRoot } else { Get-Location }

# 1. 找 MP3
if (-not $Mp3) {
    $mp3File = Get-ChildItem -Path $dir -Filter *.mp3 -File | Select-Object -First 1
    if (-not $mp3File) {
        Write-Host "错误:目录里没有 .mp3 文件。请把 mp3 放到脚本同目录,或用 -Mp3 指定。" -ForegroundColor Red
        exit 1
    }
    $Mp3 = $mp3File.FullName
}
if (-not (Test-Path -LiteralPath $Mp3)) {
    Write-Host "错误:找不到 mp3: $Mp3" -ForegroundColor Red
    exit 1
}

# 2. 找 mod jar
if ($Jar.Count -eq 0) {
    $jarList = Get-ChildItem -Path $dir -Filter *.jar -File | Where-Object { $_.Name -match '^Lapis-Zgrnf-(fabric|neoforge)-' }
    if (-not $jarList) {
        Write-Host "错误:目录里没有 Lapis-Zgrnf 的 mod jar。请把 jar 放到脚本同目录,或用 -Jar 指定。" -ForegroundColor Red
        exit 1
    }
    $Jar = @($jarList | ForEach-Object { $_.FullName })
}

# 3. 注入(jar 是 zip,把 mp3 作为 assets/zgrnf/song.mp3 写进去)
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$target = 'assets/zgrnf/song.mp3'
$songName = Split-Path $Mp3 -Leaf
$injected = 0

foreach ($jarPath in $Jar) {
    if (-not (Test-Path -LiteralPath $jarPath)) {
        Write-Host "警告:跳过不存在的 jar: $jarPath" -ForegroundColor Yellow
        continue
    }
    try {
        $zip = [System.IO.Compression.ZipFile]::Open($jarPath, 'Update')
        try {
            foreach ($e in @($zip.Entries | Where-Object { $_.FullName -eq $target })) {
                $e.Delete()
            }
            $entry = $zip.CreateEntry($target, [System.IO.Compression.CompressionLevel]::Optimal)
            $es = $entry.Open()
            $fs = [System.IO.File]::OpenRead($Mp3)
            try {
                $fs.CopyTo($es)
            } finally {
                $fs.Dispose()
                $es.Dispose()
            }
        } finally {
            $zip.Dispose()
        }
        $injected++
        Write-Host "已注入: $jarPath  <-  $songName" -ForegroundColor Green
    } catch {
        Write-Host "失败: $jarPath ($($_.Exception.Message)) - 若文件被占用,请先关闭 Minecraft 再试" -ForegroundColor Red
    }
}

if ($injected -eq 0) {
    Write-Host "没有成功注入任何 jar,请检查上面的错误。" -ForegroundColor Yellow
    exit 1
}

Write-Host "完成。把注入后的 jar 放进 mods 文件夹,进游戏按 G 打开播放器、按 Y 播放。" -ForegroundColor Cyan
