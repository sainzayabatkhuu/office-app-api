package com.sol.office.identity.service;

import com.sol.office.common.api.PageResponse;
import com.sol.office.common.exception.ConflictException;
import com.sol.office.common.exception.NotFoundException;
import com.sol.office.identity.domain.CoreParameter;
import com.sol.office.identity.dto.ParameterDtos.CoreParameterRequest;
import com.sol.office.identity.dto.ParameterDtos.CoreParameterResponse;
import com.sol.office.identity.repository.CoreParameterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * System parameters (CORPRM). Type + code is unique.
 */
@Service
@RequiredArgsConstructor
public class CoreParameterService {

    private final CoreParameterRepository repository;

    /** All filters optional: type is an exact match, code and name a case-insensitive "contains". */
    @Transactional(readOnly = true)
    public PageResponse<CoreParameterResponse> search(String type, String code, String name, Pageable pageable) {
        Specification<CoreParameter> spec = (root, q, cb) -> cb.conjunction();
        if (hasText(type)) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("paramType"), type.trim()));
        }
        if (hasText(code)) {
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("paramCode")), like(code)));
        }
        if (hasText(name)) {
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("paramName")), like(name)));
        }
        return PageResponse.of(repository.findAll(spec, pageable), CoreParameterService::toResponse);
    }

    @Transactional(readOnly = true)
    public CoreParameterResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public CoreParameterResponse create(CoreParameterRequest request) {
        if (repository.existsByParamTypeAndParamCode(request.type().trim(), request.code().trim())) {
            throw new ConflictException("Parameter already exists: " + request.type() + "/" + request.code());
        }
        CoreParameter parameter = new CoreParameter();
        apply(parameter, request);
        return toResponse(repository.save(parameter));
    }

    @Transactional
    public CoreParameterResponse update(Long id, CoreParameterRequest request) {
        CoreParameter parameter = find(id);
        boolean keyChanged = !parameter.getParamType().equals(request.type().trim())
                || !parameter.getParamCode().equals(request.code().trim());
        if (keyChanged && repository.existsByParamTypeAndParamCode(request.type().trim(), request.code().trim())) {
            throw new ConflictException("Parameter already exists: " + request.type() + "/" + request.code());
        }
        apply(parameter, request);
        return toResponse(parameter);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private CoreParameter find(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("Parameter", id));
    }

    private static void apply(CoreParameter parameter, CoreParameterRequest request) {
        parameter.setParamType(request.type().trim());
        parameter.setParamCode(request.code().trim());
        parameter.setParamName(request.name().trim());
    }

    private static CoreParameterResponse toResponse(CoreParameter p) {
        return new CoreParameterResponse(p.getId(), p.getParamType(), p.getParamCode(), p.getParamName(), "N");
    }

    static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    static String like(String s) {
        return "%" + s.trim().toLowerCase() + "%";
    }
}
