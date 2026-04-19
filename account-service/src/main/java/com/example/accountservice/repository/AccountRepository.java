package com.example.accountservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {

    Optional<Account> findByUsernameAndVisible(String username, int visible);

    // Optional<Account> findByUsernameAndVisible(String username, int visible);

    boolean existsByCccdAndVisible(String cccd, int visible);

    boolean existsByEmailAndVisible(String email, int visible);

    boolean existsByUsernameAndVisible(String username, int visible);

    Optional<Account> findByCccdAndVisible(String cccd, int visible);

    Optional<Account> findByEmailAndVisible(String email, int visible);

    Optional<Account> findByCccdAndVisibleAndRole(String cccd, int visible, Role role);

    // OAuth2 methods
    Optional<Account> findByGoogleIdAndVisible(String googleId, int visible);

    boolean existsByGoogleIdAndVisible(String googleId, int visible);

}