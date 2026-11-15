package com.sol.office.identity.web;

import com.sol.office.common.api.PageResponse;
import com.sol.office.identity.dto.ParameterDtos.CoreParameterRequest;
import com.sol.office.identity.dto.ParameterDtos.CoreParameterResponse;
import com.sol.office.identity.service.CoreParameterService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

@Tag(name = "System parameters")
@RestController
@RequestMapping("/api/v1/core-parameters")
@RequiredArgsConstructor
public class CoreParameterController {

    private final CoreParameterService service;

    @GetMapping
    @PreAuthorize("hasAuthority('PARAM_VIEW')")
    public PageResponse<CoreParameterResponse> search(
            @RequestParam(required = false) String paramType,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 10, sort = {"paramType", "paramCode"}, direction = Sort.Direction.ASC) Pageable pageable) {
        return service.search(paramType, code, name, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PARAM_VIEW')")
    public CoreParameterResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PARAM_MANAGE')")
    public CoreParameterResponse create(@Valid @RequestBody CoreParameterRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PARAM_MANAGE')")
    public CoreParameterResponse update(@PathVariable Long id, @Valid @RequestBody CoreParameterRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PARAM_MANAGE')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
