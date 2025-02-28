package com.mahitotsu.steropes.api.controller.account;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;

@RestController
@RequestMapping(path = "/api/accoount/open")
public class OpenAccountController {

    @Data
    public static class OpenRequest {
        private String branchNumber;
    }

    @Data
    public static class OpenResponse {
        private String branchNumber;
        private String accountNumber;
    }

    @PostMapping
    public OpenResponse openAccount(@RequestBody final OpenRequest request) {

        final OpenResponse response = new OpenResponse();
        response.setBranchNumber(request.getBranchNumber());
        return response;
    }
}
