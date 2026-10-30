$ErrorActionPreference = 'Stop'
$dir = Join-Path $env:TEMP ('pixelGram-setup-' + [guid]::NewGuid().ToString('N'))
try {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::ExtractToDirectory((Join-Path $PSScriptRoot 'payload.zip'), $dir)
    $exe = Get-ChildItem -LiteralPath $dir -Recurse -Filter 'pixelGram Setup.exe' | Select-Object -First 1
    $msi = Get-ChildItem -LiteralPath $dir -Recurse -Filter 'pixelGram.msi' | Select-Object -First 1
    Start-Process -FilePath $exe.FullName -ArgumentList @('--msi', ('"' + $msi.FullName + '"')) -WorkingDirectory $exe.DirectoryName -Wait
} finally {
    Remove-Item -LiteralPath $dir -Recurse -Force -ErrorAction SilentlyContinue
}
