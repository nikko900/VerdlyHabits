# Compress focus ambient tracks (optional) and upload to Firebase Storage.
# Requires: ffmpeg in PATH, Firebase CLI logged in (`firebase login`).
#
# Compression (~154MB -> ~15-25MB): mono, 64kbps, 3-minute loops — fine for background focus audio.
# Upload path in Storage: ambient_sounds/{filename}.mp3

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$sources = Join-Path $PSScriptRoot "ambient_sources"
$compressed = Join-Path $PSScriptRoot "ambient_compressed"
New-Item -ItemType Directory -Force -Path $compressed | Out-Null

if (-not (Get-Command ffmpeg -ErrorAction SilentlyContinue)) {
    Write-Host "ffmpeg not found. Install: winget install Gyan.FFmpeg"
    Write-Host "Or upload originals from tools/ambient_sources without compression."
    exit 1
}

Get-ChildItem $sources -Filter "ambient_*.mp3" | ForEach-Object {
    $out = Join-Path $compressed $_.Name
    Write-Host "Compressing $($_.Name)..."
    ffmpeg -y -i $_.FullName -vn -t 180 -ac 1 -ar 44100 -b:a 64k -map_metadata -1 $out 2>$null
}

$totalMb = [math]::Round((Get-ChildItem $compressed | Measure-Object Length -Sum).Sum / 1MB, 1)
Write-Host "Compressed total: ${totalMb} MB in $compressed"

Write-Host ""
Write-Host "Upload to Firebase (replace YOUR_BUCKET):"
Get-ChildItem $compressed -Filter "*.mp3" | ForEach-Object {
    Write-Host "  gsutil cp `"$($_.FullName)`" gs://YOUR_BUCKET/ambient_sounds/$($_.Name)"
}
Write-Host ""
Write-Host "Or Firebase Console: Storage -> ambient_sounds/ -> upload all files from ambient_compressed"
