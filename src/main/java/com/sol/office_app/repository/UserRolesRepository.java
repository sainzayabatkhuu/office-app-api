package com.sol.office_app.repository;

import com.sol.office_app.entity.User;
import com.sol.office_app.entity.UserRoles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRolesRepository extends JpaRepository<UserRoles, Long> {
//    List<UserRoles> findByUser(User user);

//    @Query("select ur from UserRoles as ur where ur.user.email = :email")
//    List<UserRoles> findByUserEmail(@Param("email") String email);
//
//    @Query(nativeQuery = true, value="SELECT * from users_roles as ur where ur.user_id = :id")
//    List<UserRoles> findByUserEmailRaw(@Param("id") Long id);

    Page<UserRoles> findByUser(User user, Pageable pageable);
}
