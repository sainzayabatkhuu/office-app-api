package com.sol.office_app.repository;

import com.sol.office_app.entity.CoreParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CoreParameterRepository extends
        JpaRepository<CoreParameter, Long>,
        JpaSpecificationExecutor<CoreParameter> {
}
