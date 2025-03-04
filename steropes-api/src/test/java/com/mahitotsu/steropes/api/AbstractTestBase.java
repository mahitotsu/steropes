package com.mahitotsu.steropes.api;

import java.util.Random;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.annotation.PostConstruct;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class AbstractTestBase {

    private Random random = new Random();

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

    protected String randomBranchNumber() {
        return String.format("%03d", this.random.nextInt(1000));
    }

    protected String randomAccountNumber() {
        return String.format("%07d", this.random.nextInt(10000000));
    }
}
