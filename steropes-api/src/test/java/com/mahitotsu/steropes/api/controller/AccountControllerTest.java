package com.mahitotsu.steropes.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.mahitotsu.steropes.api.TestMain;
import com.mahitotsu.steropes.api.controller.AccountController.AccountId;
import com.mahitotsu.steropes.api.controller.AccountController.AccountResponse;
import com.mahitotsu.steropes.api.controller.AccountController.OpenAccountReqest;
import com.mahitotsu.steropes.api.controller.AccountController.TransactAccountRequest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class AccountControllerTest extends TestMain {

    private static final Random RANDOM = new Random();

    private String randomBranchNumber() {
        return String.format("%03d", RANDOM.nextInt(1000));
    }

    @Test
    public void testOpenAccount() {

        final OpenAccountReqest request = new OpenAccountReqest();
        request.setBranchNumber(this.randomBranchNumber());
        request.setMaxBalance(1000);

        final Response response = RestAssured.given().when()
                .contentType(ContentType.JSON).body(request)
                .post("/account/open");
        response.then().statusCode(200);

        final AccountResponse account = response.body().as(AccountResponse.class);
        assertEquals(request.getBranchNumber(), account.getBranchNumber());
        assertNotNull(account.getAccountNumber());
        assertEquals(1000, account.getMaxBalance());
        assertEquals(0, account.getCurrentBalance());
    }

    @Test
    public void testTransactAccount() {

        final String branchNumber = this.randomBranchNumber();
        final long maxBalance = 1000;
        AccountResponse account;

        // ----- open
        final OpenAccountReqest openRequest = new OpenAccountReqest();
        openRequest.setBranchNumber(branchNumber);
        openRequest.setMaxBalance(maxBalance);

        final Response openResponse = RestAssured.given().when()
                .contentType(ContentType.JSON).body(openRequest)
                .post("/account/open");
        openResponse.then().statusCode(200);
        account = openResponse.body().as(AccountResponse.class);
        assertEquals(0, account.getCurrentBalance());

        //
        final AccountId accountId = new AccountId();
        accountId.setBranchNumber(account.getBranchNumber());
        accountId.setAccountNumber(account.getAccountNumber());

        // ----- deposit
        final TransactAccountRequest depositRequest = new TransactAccountRequest();
        depositRequest.setAccount(accountId);
        depositRequest.setAmount(500);

        final Response depositResponse = RestAssured.given().when()
                .contentType(ContentType.JSON).body(depositRequest)
                .post("/account/deposit");
        depositResponse.then().statusCode(200);
        account = depositResponse.body().as(AccountResponse.class);
        assertEquals(500, account.getCurrentBalance());

        // ----- withdraw
        final TransactAccountRequest withdrawRequest = new TransactAccountRequest();
        withdrawRequest.setAccount(accountId);
        withdrawRequest.setAmount(300);

        final Response withdrawResponse = RestAssured.given().when()
                .contentType(ContentType.JSON).body(withdrawRequest)
                .post("/account/withdraw");
        withdrawResponse.then().statusCode(200);
        account = withdrawResponse.body().as(AccountResponse.class);
        assertEquals(200, account.getCurrentBalance());

        // ----- get
        final Response accountResponse = RestAssured.given().when()
                .contentType(ContentType.JSON).body(accountId)
                .post("/account/get");
        accountResponse.then().statusCode(200);
        account = accountResponse.body().as(AccountResponse.class);
        assertEquals(200, account.getCurrentBalance());
    }
}
