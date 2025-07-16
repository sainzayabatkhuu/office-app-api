package com.sol.office_app.controller;

import com.sol.office_app.common.Constant;
import com.sol.office_app.dto.RoleDTO;
import com.sol.office_app.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(Constant.ROLE_URL_PREFIX)
public class RoleController {

    @Autowired
    private RoleService ruleService;

    @GetMapping()
    public ResponseEntity<Page<RoleDTO>> index(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size){
        return new ResponseEntity<>(ruleService.findAll(PageRequest.of(page, size)), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDTO> get(@PathVariable("id") Long id){
        return ruleService.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<RoleDTO> save(@RequestBody RoleDTO roleDTO) {
        Optional<RoleDTO> savedRule = ruleService.save(roleDTO);
        return savedRule.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleDTO> update(@PathVariable Long id, @RequestBody RoleDTO rulesDTO) {
        Optional<RoleDTO> updatedRuleOpt = ruleService.update(id, rulesDTO);
        return updatedRuleOpt
                .map(roleDTO -> new ResponseEntity<>(roleDTO, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RoleDTO> delete(@PathVariable Long id, @RequestBody RoleDTO rulesDTO) {
        Optional<RoleDTO> updatedRuleOpt = ruleService.delete(id, rulesDTO);
        return updatedRuleOpt
                .map(roleDTO -> new ResponseEntity<>(roleDTO, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

}
