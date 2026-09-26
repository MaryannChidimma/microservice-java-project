package com.example.demo.service;

import com.example.demo.dto.UserDto;
import com.example.demo.model.UserModel;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
}