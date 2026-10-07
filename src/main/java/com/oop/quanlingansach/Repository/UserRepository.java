package com.oop.quanlingansach.Repository;

import com.oop.quanlingansach.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByRole(User.Role role);

    List<User> findByRoleIn(Collection<User.Role> roles);

    List<User> findAllByOrderByRoleAscUsernameAsc();

    long countByRole(User.Role role);
}
