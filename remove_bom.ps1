$testDir = "c:\123321\!!misha\tunduk-misha\src\test\java\org\misha\authservice"
$files = Get-ChildItem -Path $testDir -Recurse -Filter "*.java"
$utf8NoBom = New-Object System.Text.UTF8Encoding($False)

foreach ($f in $files) {
    $bytes = [System.IO.File]::ReadAllBytes($f.FullName)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        $content = [System.IO.File]::ReadAllText($f.FullName)
        [System.IO.File]::WriteAllText($f.FullName, $content, $utf8NoBom)
        Write-Host "Removed BOM from $($f.Name)"
    }
}
Write-Host "BOM removal complete."
