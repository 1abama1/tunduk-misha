$path = "c:\123321\!!misha\tunduk-misha\src\main\java"
Get-ChildItem -Path $path -Filter *.java -Recurse | ForEach-Object {
    $bytes = Get-Content -Path $_.FullName -Encoding Byte -TotalCount 3 -ErrorAction SilentlyContinue
    if ($bytes.Count -eq 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        Write-Host "Fixing BOM in $($_.FullName)"
        $content = Get-Content -Path $_.FullName -Encoding UTF8
        [System.IO.File]::WriteAllLines($_.FullName, $content, (New-Object System.Text.UTF8Encoding($false)))
    }
}
