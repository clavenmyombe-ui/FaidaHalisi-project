package com.smart.faidahalisi.Repository;
import com.smart.faidahalisi.Model.User;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long>{
Optional<User> findByEmail(String email);
}