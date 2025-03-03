package com.mahitotsu.steropes.api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.annotation.PostConstruct;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class AbstractTestBase {

    @LocalServerPort
    private int port;

    private RequestSpecification baseRequestSpecification;

    @PostConstruct
    public void setup() {
        this.baseRequestSpecification = RestAssured.given().baseUri("http://localhost").port(this.port)
                .contentType(ContentType.JSON);
    }

    protected RequestSpecification getBaseRequestSpecification() {
        return this.baseRequestSpecification;
    }
}
