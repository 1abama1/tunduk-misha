$projectRoot = "c:\123321\!!misha\tunduk-misha"
$controllerDir = "$projectRoot\src\main\java\org\misha\authservice\controller"
$testDir = "$projectRoot\src\test\java\org\misha\authservice\controller"
$utf8NoBom = New-Object System.Text.UTF8Encoding($False)

$controllers = Get-ChildItem -Path $controllerDir -Filter "*.java"

foreach ($c in $controllers) {
    $cContent = [System.IO.File]::ReadAllText($c.FullName)
    $testFile = "$testDir\$($c.BaseName)Test.java"
    if (-not (Test-Path $testFile)) { continue }
    
    $testContent = [System.IO.File]::ReadAllText($testFile)
    
    $matches = [regex]::Matches($cContent, 'private\s+final\s+([A-Z]\w+)\s+(\w+)\s*;')
    
    $mocksToAdd = ""
    foreach ($m in $matches) {
        $type = $m.Groups[1].Value
        $name = $m.Groups[2].Value
        
        # skip if it's already in the test
        if (-not [regex]::IsMatch($testContent, "private\s+$type\s+$name;")) {
            $mocksToAdd += "`r`n    @MockBean`r`n    private $type $name;"
        }
    }
    
    if ($mocksToAdd -ne "") {
        # Insert mocks before @Test
        $testContent = $testContent -replace '(\s+)@Test', "$mocksToAdd`$1@Test"
        
        # Add import for services if not exists
        if (-not $testContent.Contains("import org.misha.authservice.service.*;")) {
            $testContent = $testContent -replace 'import org\.junit\.jupiter\.api\.Test;', "import org.junit.jupiter.api.Test;`r`nimport org.misha.authservice.service.*;"
        }
        
        [System.IO.File]::WriteAllText($testFile, $testContent, $utf8NoBom)
        Write-Host "Updated $($c.BaseName)Test.java"
    }
}
Write-Host "Done"
