package com.sol.office_app.controller;

import com.sol.office_app.common.Constant;
import com.sol.office_app.dto.CustomerDTO;
import com.sol.office_app.enums.CustomerType;
import com.sol.office_app.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constant.CUSTOMER_URL_PREFIX)
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public Page<CustomerDTO> index(@RequestParam(defaultValue = "") String customerNumber,
                                   @RequestParam(defaultValue = "") String name,
                                   @RequestParam(required = false) CustomerType customerType,
                                   @RequestParam(defaultValue = "") String branchCode,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        return customerService.search(customerNumber, name, customerType, branchCode,
                PageRequest.of(page, size, Sort.by("customerNumber")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> get(@PathVariable("id") String id) {
        return customerService.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<CustomerDTO> save(@RequestBody CustomerDTO customer) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(customer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> update(@PathVariable String id, @RequestBody CustomerDTO customerDTO) {
        return customerService.update(id, customerDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
