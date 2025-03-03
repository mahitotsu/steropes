package com.mahitotsu.steropes.api.controller.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.mahitotsu.steropes.api.AbstractTestBase;
import com.mahitotsu.steropes.api.controller.account.OpenAccountController.OpenRequest;
import com.mahitotsu.steropes.api.controller.account.OpenAccountController.OpenResponse;

import io.restassured.response.Response;

public class OpenAccountControllerTest extends AbstractTestBase {

    @Test
    public void testOpenAccount() {

        final String branchNUmber = "001";
        final OpenRequest data = new OpenRequest();
        data.setBranchNumber(branchNUmber);

        final Response response = this.getBaseRequestSpecification()
                .basePath("/api/account/open").body(data)
                .when()
                .post().thenReturn();
        assertEquals(200, response.statusCode());

        final OpenResponse body = response.getBody().as(OpenResponse.class);
        assertNotNull(body);
        assertEquals(branchNUmber, body.getBranchNumber());
    }
}
