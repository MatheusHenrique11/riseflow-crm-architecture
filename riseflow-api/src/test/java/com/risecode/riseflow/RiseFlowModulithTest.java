package com.risecode.riseflow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class RiseFlowModulithTest {
    @Test
    void shouldVerifyApplicationModuleBoundaries() {
        ApplicationModules.of(RiseFlowApplication.class).verify();
    }
}
