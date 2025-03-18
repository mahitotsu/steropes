package com.mahitotsu.steropes.api.controller;

import org.junit.jupiter.api.Test;

import com.mahitotsu.steropes.api.TestMain;

import io.restassured.RestAssured;
import io.restassured.response.Response;

public class ActuatorControllerTest extends TestMain {
    
    @Test
    public void testHealth() {
        final Response response = RestAssured.given().when().get("/actuator/health");
        response.then().statusCode(200);
    }
}
