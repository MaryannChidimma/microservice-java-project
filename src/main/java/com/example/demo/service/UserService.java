package com.example.demo.service;

import com.example.demo.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

@Service 
public class UserService {
  private final List<User> users = new ArrayList<>();
  private final AtomicLong idCounter = new AtomicLong();

  public List<User> getAllUsers() {
    return users;
  }

  public User getUserById(Long id) {
      return users.stream().filter( user -> user.getId().equals(id)).findFirst().orElse(null);
  }

  public User createUser(User user){

    user.setId(idCounter.incrementAndGet());
    users.add(user);
    return user;
  }

  public User updateUser(Long id, User updatedUser) {
    User existing = getUserById(id);
    if(existing != null) {
        existing.setName(updatedUser.getName());
        existing.setEmail(updatedUser.getEmail());
    }
    return existing;
  }

  public boolean deleteUser(Long id) {
    return users.removeIf(user -> user.getId().equals(id));
  }

}

