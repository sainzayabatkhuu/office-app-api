package com.sol.office.identity.web;

import com.sol.office.common.api.PageResponse;
import com.sol.office.common.security.SecurityUtils;
import com.sol.office.identity.domain.CustomerType;
import com.sol.office.identity.dto.CustomerDtos.CustomerDto;
import com.sol.office.identity.service.CustomerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Customers")
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public PageResponse<CustomerDto> search(
            @RequestParam(required = false) String customerNumber,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) CustomerType customerType,
            @RequestParam(required = false) String branchCode,
            @PageableDefault(size = 10, sort = "customerNumber", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.search(SecurityUtils.requireCurrentUser(), customerNumber, name, customerType, branchCode, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public CustomerDto get(@PathVariable String id) {
        return service.get(SecurityUtils.requireCurrentUser(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
    public CustomerDto create(@RequestBody CustomerDto request) {
        return service.create(SecurityUtils.requireCurrentUser(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
    public CustomerDto update(@PathVariable String id, @RequestBody CustomerDto request) {
        return service.update(SecurityUtils.requireCurrentUser(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
    public void delete(@PathVariable String id) {
        service.delete(SecurityUtils.requireCurrentUser(), id);
    }
}
