package com.mahitotsu.steropes.api.controller.actuator;

import org.junit.jupiter.api.Test;

import com.mahitotsu.steropes.api.AbstractTestBase;

public class ActuatorTest extends AbstractTestBase {

    @Test
    public void testHealth() {

        this.getBaseRequestSpecification().basePath("/actuator/health")
                .when().get()
                .then().statusCode(200);
    }
}
