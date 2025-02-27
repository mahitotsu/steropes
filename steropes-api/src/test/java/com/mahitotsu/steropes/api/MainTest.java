package com.mahitotsu.steropes.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

public class MainTest extends AbstractTestBase {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testActuatorHealth() {
        this.webTestClient.get().uri("/actuator/health").exchange().expectStatus().isOk();
    }
}
