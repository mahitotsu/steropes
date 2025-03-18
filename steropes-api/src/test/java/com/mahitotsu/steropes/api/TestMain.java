package com.mahitotsu.steropes.api;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import com.mahitotsu.steropes.api.config.DataConfig;

import io.restassured.RestAssured;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({ DataConfig.class })
public abstract class TestMain {

    private static volatile Boolean restAssuredInit = Boolean.FALSE;

    @LocalServerPort
    private int localServerPort;

    @BeforeEach
    public void setup() {
        if (Boolean.TRUE.equals(restAssuredInit)) {
            return;
        }
        synchronized (RestAssured.class) {
            if (Boolean.TRUE.equals(restAssuredInit)) {
                return;
            }
            RestAssured.port = this.localServerPort;
            RestAssured.baseURI = "http://localhost";
            RestAssured.basePath = "";
            restAssuredInit = Boolean.TRUE;
        }
    }
}