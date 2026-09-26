package com.example.userservice.service;

import com.example.userservice.dto.UserDto;
import com.example.userservice.model.UserModel;
import com.example.userservice.repository.AddressRepository;
import com.example.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    public UserService(UserRepository userRepository, AddressRepository addressRepository) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
    }

    private UserDto toDto(UserModel user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail());
    }

    private UserModel toEntity(UserDto dto) {
        return new UserModel(dto.getName(), dto.getEmail());
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public UserDto getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::toDto)
                .orElse(null);
    }

    public UserDto createUser(UserDto dto) {
        UserModel saved = userRepository.save(toEntity(dto));
        return toDto(saved);
    }

    public UserDto updateUser(Long id, UserDto dto) {
        return userRepository.findById(id)
                .map(existing -> {
                    existing.setName(dto.getName());
                    existing.setEmail(dto.getEmail());
                    return toDto(userRepository.save(existing));
                })
                .orElse(null);
    }

    @Transactional
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            // Address holds the FK to users, so remove it first
            addressRepository.findByUser_Id(id).ifPresent(addressRepository::delete);
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
