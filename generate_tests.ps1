$projectRoot = "c:\123321\!!misha\tunduk-misha"
$srcDir = "$projectRoot\src\main\java\org\misha\authservice"
$testDir = "$projectRoot\src\test\java\org\misha\authservice"

# Create directories if they don't exist
New-Item -ItemType Directory -Force -Path "$testDir\service" | Out-Null
New-Item -ItemType Directory -Force -Path "$testDir\controller" | Out-Null

# 1. Services
$serviceFiles = Get-ChildItem -Path "$srcDir\service" -Filter "*.java"
foreach ($file in $serviceFiles) {
    $className = $file.BaseName
    $testClassName = "${className}Test"
    $testFilePath = "$testDir\service\${testClassName}.java"

    if (-not (Test-Path $testFilePath)) {
        $content = @"
package org.misha.authservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ${testClassName} {

    @InjectMocks
    private ${className} service;

    @Test
    void testContextLoads() {
        assertNotNull(service);
    }
}
"@
        Set-Content -Path $testFilePath -Value $content -Encoding UTF8
        Write-Host "Created service test: $testFilePath"
    } else {
        Write-Host "Skipped existing service test: $testFilePath"
    }
}

# 2. Controllers
$controllerFiles = Get-ChildItem -Path "$srcDir\controller" -Filter "*.java"
foreach ($file in $controllerFiles) {
    $className = $file.BaseName
    $testClassName = "${className}Test"
    $testFilePath = "$testDir\controller\${testClassName}.java"

    if (-not (Test-Path $testFilePath)) {
        $content = @"
package org.misha.authservice.controller;

import org.junit.jupiter.api.Test;
import org.misha.authservice.security.EmailPhoneAuthenticationProvider;
import org.misha.authservice.security.JwtFilter;
import org.misha.authservice.security.JwtUtil;
import org.misha.authservice.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebMvcTest(
        controllers = ${className}.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtFilter.class, EmailPhoneAuthenticationProvider.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class ${testClassName} {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void testContextLoads() {
        assertNotNull(mockMvc);
    }
}
"@
        Set-Content -Path $testFilePath -Value $content -Encoding UTF8
        Write-Host "Created controller test: $testFilePath"
    } else {
        Write-Host "Skipped existing controller test: $testFilePath"
    }
}

Write-Host "Done!"
