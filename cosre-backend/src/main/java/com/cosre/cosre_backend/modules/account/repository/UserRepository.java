package com.cosre.cosre_backend.modules.account.repository;

import com.cosre.cosre_backend.modules.account.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import com.cosre.cosre_backend.common.constants.RoleEnum;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByRoleAndIsActiveTrueOrderByFullName(RoleEnum role);
}
