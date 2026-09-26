package com.example.demo.repository;

import com.example.demo.model.AddressModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<AddressModel, Long> {
    Optional<AddressModel> findByUser_Id(Long userId);
}