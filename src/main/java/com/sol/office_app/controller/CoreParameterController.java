package com.sol.office_app.controller;

import com.sol.office_app.common.Constant;
import com.sol.office_app.domain.response.CoreParameterResponse;
import com.sol.office_app.service.CoreParameterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(Constant.CORE_PARAM_URL_PREFIX)
public class CoreParameterController {
    private final CoreParameterService service;

    public CoreParameterController(CoreParameterService service) {
        this.service = service;
    }

    @GetMapping
    public Page<CoreParameterResponse> getAll(
            @RequestParam(defaultValue = "") String paramType,
            @RequestParam(defaultValue = "") String code,
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.findDynamic( paramType, code, name, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CoreParameterResponse> getById(@PathVariable Long id) {
        return service.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<CoreParameterResponse> create(@RequestBody CoreParameterResponse parameter) {
        Optional<CoreParameterResponse>  savedParam =  service.save(parameter);
        return service.save(parameter)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CoreParameterResponse> update(
            @PathVariable Long id,
            @RequestBody CoreParameterResponse parameter) {
        return service.update(id, parameter)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
