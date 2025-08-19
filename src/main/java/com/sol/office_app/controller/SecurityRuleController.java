package com.sol.office_app.controller;

import com.sol.office_app.dto.SecurityRuleDTO;
import com.sol.office_app.service.SecurityRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import com.sol.office_app.common.Constant;

@RestController
@RequestMapping(Constant.SECURITY_RULE_URL_PREFIX)
public class SecurityRuleController {

    @Autowired
    private SecurityRuleService securityRuleService;

    @GetMapping()
    public ResponseEntity<Page<SecurityRuleDTO>> index(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size){
        return new ResponseEntity<>(securityRuleService.findAll(PageRequest.of(page, size)), HttpStatus.OK);
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<SecurityRuleDTO> get(@PathVariable("id") Long id){
        return securityRuleService.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<SecurityRuleDTO> save(@RequestBody SecurityRuleDTO user) {
        Optional<SecurityRuleDTO> savedUser = securityRuleService.save(user);
        return savedUser.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<SecurityRuleDTO> update(@PathVariable Long id, @RequestBody SecurityRuleDTO securityRuleDTO) {
        Optional<SecurityRuleDTO> updatedSecurityRuleOpt = securityRuleService.update(id, securityRuleDTO);
        return updatedSecurityRuleOpt
                .map(SecurityRuleDTO -> new ResponseEntity<>(SecurityRuleDTO, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<SecurityRuleDTO> delete(@PathVariable Long id) {
        securityRuleService.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
