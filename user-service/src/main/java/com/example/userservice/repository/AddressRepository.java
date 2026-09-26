package com.example.userservice.repository;

import com.example.userservice.model.AddressModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<AddressModel, Long> {
    Optional<AddressModel> findByUser_Id(Long userId);
}
