package com.sol.office_app.repository;

import com.sol.office_app.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {

    @Query("select b from Branch as b inner join UserRoles as ur ON b.solId = ur.branch.solId where ur.user.email = :username ORDER BY b.solId asc")
    List<Branch> findByUsername(@Param("username") String username);
}
