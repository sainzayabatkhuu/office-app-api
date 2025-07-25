package com.sol.office_app.controller;

import com.sol.office_app.common.Constant;
import com.sol.office_app.dto.BranchDTO;
import com.sol.office_app.service.BranchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(Constant.BRANCH_URL_PREFIX)
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<BranchDTO> index(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size){
        Page<BranchDTO> pageData= branchService.findAll(PageRequest.of(page, size));
        return pageData;
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchDTO> get(@PathVariable("id") String id){
        return branchService.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<BranchDTO> save(@RequestBody BranchDTO branchDTO) {
        Optional<BranchDTO> savedBranch = branchService.save(branchDTO);
        return savedBranch.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<BranchDTO> update(@PathVariable String id, @RequestBody BranchDTO branchsDTO) {
        Optional<BranchDTO> updatedBranchOpt = branchService.update(id, branchsDTO);
        return updatedBranchOpt
                .map(branchDTO -> new ResponseEntity<>(branchDTO, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BranchDTO> delete(@PathVariable String id, @RequestBody BranchDTO branchsDTO) {
        Optional<BranchDTO> updatedBranchOpt = branchService.delete(id, branchsDTO);
        return updatedBranchOpt
                .map(branchDTO -> new ResponseEntity<>(branchDTO, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
