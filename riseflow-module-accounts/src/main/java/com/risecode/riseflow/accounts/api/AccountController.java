package com.risecode.riseflow.accounts.api;

import com.risecode.riseflow.accounts.search.SearchRequest;
import com.risecode.riseflow.core.dto.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_account:write')")
    ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountRequest request) {
        AccountResponse response = accountService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_account:read')")
    AccountResponse get(@PathVariable UUID id) {
        return accountService.get(id);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_account:read')")
    List<AccountResponse> list() {
        return accountService.list();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_account:write')")
    AccountResponse update(@PathVariable UUID id, @Valid @RequestBody AccountRequest request) {
        return accountService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_account:write')")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('SCOPE_account:read')")
    PageResponse<AccountResponse> search(@Valid @RequestBody SearchRequest request) {
        return accountService.searchAccounts(request.criteria(), request.page(), request.size());
    }
}
