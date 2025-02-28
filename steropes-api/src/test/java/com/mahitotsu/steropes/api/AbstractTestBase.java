package com.mahitotsu.steropes.api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import jakarta.annotation.PostConstruct;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractTestBase {

    @LocalServerPort
    private int port;

    private RequestSpecification baseRequestSpecification;

    @PostConstruct
    public void setup() {
        this.baseRequestSpecification = RestAssured.given().baseUri("http://localhost").port(this.port);
    }

    protected RequestSpecification getBaseRequestSpecification() {
        return this.baseRequestSpecification;
    }
}
