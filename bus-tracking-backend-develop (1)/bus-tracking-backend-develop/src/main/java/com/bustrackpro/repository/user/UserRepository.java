package com.bustrackpro.repository.user;

import com.bustrackpro.modal.user.UserRole;
import com.bustrackpro.modal.user.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User repository.
 */
@Repository
public interface UserRepository extends JpaRepository<Users, Integer> {

    Optional<Users> findByEmail(String email);

    Boolean existsByEmail(String email);

    List<Users> findByRole(UserRole role);
}
