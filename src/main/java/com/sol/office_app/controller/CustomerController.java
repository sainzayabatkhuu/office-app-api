package com.sol.office_app.controller;

import com.sol.office_app.common.Constant;
import com.sol.office_app.dto.CustomerDTO;
import com.sol.office_app.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(Constant.CUSTOMER_URL_PREFIX)
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping()
    public ResponseEntity<Page<CustomerDTO>> index(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(customerService.findAll(PageRequest.of(page, size)), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> get(@PathVariable("id") Long id){
        return customerService.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<CustomerDTO> save(@RequestBody CustomerDTO user) {
        Optional<CustomerDTO> savedUser = customerService.save(user);
        return savedUser.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> update(@PathVariable Long id, @RequestBody CustomerDTO customerDTO) {
        Optional<CustomerDTO> updatedUserOpt = customerService.update(id, customerDTO);
        return updatedUserOpt
                .map(userDTO -> new ResponseEntity<>(userDTO, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CustomerDTO> delete(@PathVariable Long id, @RequestBody CustomerDTO customerDTO) {
        Optional<CustomerDTO> updatedUserOpt = customerService.delete(id, customerDTO);
        return updatedUserOpt
                .map(userDTO -> new ResponseEntity<>(userDTO, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
