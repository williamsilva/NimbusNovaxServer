package com.nimbusnovax.common.security;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDirectoryRepository extends JpaRepository<UserDirectoryEntity, UUID> {
}
