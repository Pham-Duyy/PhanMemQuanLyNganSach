package com.oop.quanlingansach.Config;

import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DatabaseActiveUserLoader implements ActiveUserLoader {

    private final UserRepository userRepository;

    public DatabaseActiveUserLoader(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> refresh(User sessionUser) {
        return userRepository.findById(sessionUser.getId()).filter(User::isActive);
    }
}
