package org.misha.authservice.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final AdminInitializer adminInitializer;
    private final ToolDataInitializer toolDataInitializer;

    @PostConstruct
    public void init() {
        adminInitializer.init();
        toolDataInitializer.init();
    }
}
