package com.sol.office_app.controller;

import com.sol.office_app.common.Constant;
import com.sol.office_app.dto.PrivilegeDTO;
import com.sol.office_app.service.PrivilegeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(Constant.PRIVILEGE_URL_PREFIX)
public class PrivilegeController {

    @Autowired
    private PrivilegeService privilegeService;

    @GetMapping()
    public ResponseEntity<Page<PrivilegeDTO>> index(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size){
        return new ResponseEntity<>(privilegeService.findAll(PageRequest.of(page, size)), HttpStatus.OK);
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<PrivilegeDTO> get(@PathVariable("id") Long id){
        return privilegeService.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<PrivilegeDTO> save(@RequestBody PrivilegeDTO privilegeDTO) {
        Optional<PrivilegeDTO> savedPrivilege = privilegeService.save(privilegeDTO);
        return savedPrivilege.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<PrivilegeDTO> update(@PathVariable Long id, @RequestBody PrivilegeDTO privilegesDTO) {
        Optional<PrivilegeDTO> updatedPrivilegeOpt = privilegeService.update(id, privilegesDTO);
        return updatedPrivilegeOpt
                .map(privilegeDTO -> new ResponseEntity<>(privilegeDTO, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<PrivilegeDTO> delete(@PathVariable Long id) {
        privilegeService.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
