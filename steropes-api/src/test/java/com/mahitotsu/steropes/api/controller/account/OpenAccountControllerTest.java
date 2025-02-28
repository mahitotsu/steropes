package com.mahitotsu.steropes.api.controller.account;

import static org.junit.Assert.*;

import org.junit.Test;

import com.mahitotsu.steropes.api.AbstractTestBase;
import com.mahitotsu.steropes.api.controller.account.OpenAccountController.OpenResponse;

import io.restassured.response.Response;

public class OpenAccountControllerTest extends AbstractTestBase {

    @Test
    public void testOpenAccount() {

        final String branchNUmber = "001";

        final Response response = this.getBaseRequestSpecification()
                .basePath("/api/account/open").when()
                .post().thenReturn();
        assertEquals(200, response.statusCode());
        
        final OpenResponse body = response.getBody().as(OpenResponse.class);
        assertNotNull(body);
        assertEquals(branchNUmber, body.getBranchNumber());
    }
}
