package com.sol.office_app.service;

import com.sol.office_app.common.GeneralService;
import com.sol.office_app.dto.response.CoreParameterResponse;
import com.sol.office_app.entity.CoreParameter;
import com.sol.office_app.repository.CoreParameterRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CoreParameterService implements GeneralService<CoreParameterResponse, Long> {

    private final CoreParameterRepository repository;

    public CoreParameterService(CoreParameterRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<CoreParameterResponse> findAll(Pageable pageable) {
        return null;
    }

    @Override
    public Page<CoreParameterResponse> search(String query, Pageable pageable) {
        return null;
    }

    @Override
    public Optional<CoreParameterResponse> get(Long aLong) {
        return Optional.empty();
    }

    @Override
    public Optional<CoreParameterResponse> save(CoreParameterResponse entity) {
        return Optional.empty();
    }

    @Override
    public Optional<CoreParameterResponse> update(Long aLong, CoreParameterResponse entity) {
        return Optional.empty();
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<CoreParameterResponse> findDynamic(String paramType, String paramCode, String paramName, Pageable pageable) {
        Specification<CoreParameter> spec = null; // start empty

        System.out.println(paramType +" - "+ paramCode +" - "+ paramName);

        if (paramType != null && !paramType.isBlank()) {
            Specification<CoreParameter> typeSpec = (root, query, cb) ->
                    cb.equal(root.get("paramType"), paramType);
            spec = spec == null ? typeSpec : spec.and(typeSpec);
        }

        if (paramCode != null && !paramCode.isBlank()) {
            Specification<CoreParameter> codeSpec = (root, query, cb) ->
                    cb.like(cb.lower(root.get("paramCode")), "%" + paramCode.toLowerCase() + "%");
            spec = spec == null ? codeSpec : spec.and(codeSpec);
        }

        if (paramName != null && !paramName.isBlank()) {
            Specification<CoreParameter> nameSpec = (root, query, cb) ->
                    cb.like(cb.lower(root.get("paramName")), "%" + paramName.toLowerCase() + "%");
            spec = spec == null ? nameSpec : spec.and(nameSpec);
        }

        return repository.findAll(spec, pageable).map(entity -> new CoreParameterResponse(
            entity.getParamType(),
            entity.getParamCode(),
            entity.getParamName(),
            entity.getDelFlg()
        ));
    }
}
