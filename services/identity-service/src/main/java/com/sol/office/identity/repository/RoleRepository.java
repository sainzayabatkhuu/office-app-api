package com.sol.office.identity.repository;

import com.sol.office.identity.domain.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    @EntityGraph(attributePaths = "privileges")
    List<Role> findAllByOrderByCodeAsc();

    @EntityGraph(attributePaths = "privileges")
    Optional<Role> findWithPrivilegesById(Long id);

    List<Role> findByCodeIn(Collection<String> codes);

    boolean existsByCode(String code);

    @Query(value = "select count(*) > 0 from user_role where role_id = :roleId", nativeQuery = true)
    boolean isAssignedToAnyUser(@Param("roleId") Long roleId);
}
