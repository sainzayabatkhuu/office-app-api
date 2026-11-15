package com.sol.office.identity.repository;

import com.sol.office.identity.domain.Privilege;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {
    List<Privilege> findAllByOrderByModuleAscCodeAsc();

    List<Privilege> findByCodeIn(Collection<String> codes);
}
