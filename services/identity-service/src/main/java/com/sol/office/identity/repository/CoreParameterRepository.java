package com.sol.office.identity.repository;

import com.sol.office.identity.domain.CoreParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CoreParameterRepository extends JpaRepository<CoreParameter, Long>, JpaSpecificationExecutor<CoreParameter> {

    boolean existsByParamTypeAndParamCode(String paramType, String paramCode);
}
