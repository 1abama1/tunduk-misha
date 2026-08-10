$path = "c:\123321\!!misha\tunduk-misha\src\main\java\org\misha\authservice\dto"
Get-ChildItem -Path $path -Recurse -Filter *.java | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $newContent = $content -replace '\bLong toolId\b', 'java.util.UUID toolId' `
                           -replace '\bLong templateId\b', 'java.util.UUID templateId' `
                           -replace '\bLong categoryId\b', 'java.util.UUID categoryId'
    if ($content -ne $newContent) {
        Set-Content $_.FullName $newContent -NoNewline
        Write-Host "Updated $($_.Name)"
    }
}

# Fix specific DTO IDs
$filesToFixId = @(
    "AvailableToolDto.java",
    "CategoryDto.java",
    "CategoryFullDto.java",
    "TemplateDto.java",
    "TemplateFullDto.java",
    "ToolCategoryDto.java",
    "ToolFullDto.java",
    "ToolTemplateDto.java"
)
foreach ($file in $filesToFixId) {
    $matches = Get-ChildItem -Path $path -Recurse -Filter $file
    if ($matches) {
        $p = $matches[0].FullName
        $content = Get-Content $p -Raw
        $newContent = $content -replace '\bLong id\b', 'java.util.UUID id'
        if ($content -ne $newContent) {
            Set-Content $p $newContent -NoNewline
            Write-Host "Updated ID in $file"
        }
    }
}
