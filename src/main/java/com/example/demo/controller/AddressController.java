package com.example.demo.controller;

import com.example.demo.dto.AddressDto;
import com.example.demo.service.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/{userId}/address")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<AddressDto> getAddress(@PathVariable Long userId) {
        AddressDto dto = addressService.getAddressByUserId(userId);
        return dto == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
    }

    @PutMapping
    public ResponseEntity<AddressDto> saveAddress(@PathVariable Long userId, @RequestBody AddressDto dto) {
        return ResponseEntity.ok(addressService.saveAddress(userId, dto));
    }
}