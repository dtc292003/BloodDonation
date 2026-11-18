package com.example.blooddonation.repository;

import com.example.blooddonation.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("""
    SELECT u FROM User u
    JOIN FETCH u.role
    WHERE u.userId = :userId
""")
    Optional<User> findByIdWithRole(
            @Param("userId") Integer userId
    );
}
