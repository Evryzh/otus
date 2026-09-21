package com.rev.otus.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    Optional<CustomerProfile> findByKeycloakUserId(String keycloakUserId);

    Optional<CustomerProfile> findByUsername(String username);

    Optional<CustomerProfile> findByEmail(String email);

    Optional<CustomerProfile> findByUsernameAndEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}