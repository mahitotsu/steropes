package com.mahitotsu.steropes.api.controller;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mahitotsu.steropes.api.service.AccountService;
import com.mahitotsu.steropes.api.service.AccountService.AccountInfo;

import lombok.Data;

@RestController
@RequestMapping(path = "/account", consumes = MediaType.APPLICATION_JSON_VALUE)
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Data
    public static class AccountId {
        private String branchNumber;
        private String accountNumber;
    }

    @Data
    public static class OpenAccountReqest {
        private String branchNumber;
        long maxBalance;
    }

    @Data
    public static class TransactAccountRequest {
        private AccountId account;
        private long amount;
    }

    @Data
    public static class TransferRequest {
        private AccountId source;
        private AccountId destination;
        private long amount;
    }

    @Data
    public static class AccountResponse {
        private String branchNumber;
        private String accountNumber;
        private long maxBalance;
        private long currentBalance;
    }

    @PostMapping(path = "/open")
    public AccountResponse openAccount(@RequestBody final OpenAccountReqest request) {
        final AccountInfo info = this.accountService.openAccount(request.getBranchNumber(), request.getMaxBalance());
        return this.toResponse(info);
    }

    @PostMapping(path = "/get")
    public AccountResponse getAccount(@RequestBody final AccountId request) {
        final AccountInfo info = this.accountService.getAccount(request.getBranchNumber(), request.getAccountNumber());
        return this.toResponse(info);
    }

    @PostMapping(path = "/deposit")
    public AccountResponse deposit(@RequestBody final TransactAccountRequest request) {
        this.accountService.deposit(request.getAccount().getBranchNumber(), request.getAccount().getAccountNumber(),
                request.getAmount());
        final AccountInfo info = this.accountService.getAccount(request.getAccount().getBranchNumber(),
                request.getAccount().getAccountNumber());
        return this.toResponse(info);
    }

    @PostMapping(path = "/withdraw")
    public AccountResponse withdraw(@RequestBody final TransactAccountRequest request) {
        this.accountService.withdraw(request.getAccount().getBranchNumber(), request.getAccount().getAccountNumber(),
                request.getAmount());
        final AccountInfo info = this.accountService.getAccount(request.getAccount().getBranchNumber(),
                request.getAccount().getAccountNumber());
        return this.toResponse(info);
    }

    @RequestMapping(path = "/transfer")
    public AccountResponse transfer(@RequestBody final TransferRequest request) {
        this.accountService.transfer(request.getSource().getBranchNumber(), request.getSource().getAccountNumber(),
                request.getDestination().getBranchNumber(), request.getDestination().getAccountNumber(),
                request.getAmount());
        final AccountInfo info = this.accountService.getAccount(request.getSource().getBranchNumber(),
                request.getSource().getAccountNumber());
        return this.toResponse(info);
    }

    private AccountResponse toResponse(final AccountInfo info) {
        final AccountResponse response = new AccountResponse();
        BeanUtils.copyProperties(info, response);
        return response;
    }
}
