package com.sol.office_app.repository;

import com.sol.office_app.entity.CoreParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoreParameterRepository extends JpaRepository<CoreParameter, Long>, JpaSpecificationExecutor<CoreParameter> {
    boolean existsByParamCode(String paramCode);

    Page<CoreParameter> findAllByParamType(String paramType, Pageable pageable);
}
