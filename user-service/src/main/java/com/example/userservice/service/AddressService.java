package com.example.userservice.service;

import com.example.userservice.dto.AddressDto;
import com.example.userservice.model.AddressModel;
import com.example.userservice.model.UserModel;
import com.example.userservice.repository.AddressRepository;
import com.example.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    private AddressDto toDto(AddressModel address) {
        return new AddressDto(address.getId(), address.getStreet(), address.getCity(),
                address.getState(), address.getZipCode(), address.getCountry());
    }

    public AddressDto getAddressByUserId(Long userId) {
        return addressRepository.findByUser_Id(userId).map(this::toDto).orElse(null);
    }

    public AddressDto saveAddress(Long userId, AddressDto dto) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AddressModel address = addressRepository.findByUser_Id(userId)
                .orElse(new AddressModel(dto.getStreet(), dto.getCity(), dto.getState(),
                        dto.getZipCode(), dto.getCountry(), user));

        address.setStreet(dto.getStreet());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setZipCode(dto.getZipCode());
        address.setCountry(dto.getCountry());

        return toDto(addressRepository.save(address));
    }
}
