package org.misha.authservice.repository;
import org.misha.authservice.entity.User;
import org.misha.authservice.entity.Role;
import org.misha.authservice.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    List<User> findByRole(Role role);

    @Query("select u from User u where u.role = :role and :tag member of u.tags")
    List<User> findByRoleAndTag(@Param("role") Role role, @Param("tag") Tag tag);

    @Query("""
            SELECT DISTINCT u
            FROM User u
            WHERE u.role = :role
            AND (:query IS NULL OR :query = ''
                OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :query, '%')))
            AND (:simpleMode IS NULL OR u.simpleMode = :simpleMode)
            AND (:consentPersonalData IS NULL OR u.consentPersonalData = :consentPersonalData)
            """)
    List<User> searchAdvanced(
            @Param("role") Role role,
            @Param("query") String query,
            @Param("simpleMode") Boolean simpleMode,
            @Param("consentPersonalData") Boolean consentPersonalData
    );
}
