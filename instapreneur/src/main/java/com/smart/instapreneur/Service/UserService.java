package com.smart.instapreneur.Service;

import com.smart.instapreneur.Model.User;

public interface UserService {

    // Finds one user by email.
    User findUserByEmail(String email);

    // Finds one user by id.
    User findUserById(Long id);

    // Deletes one user by id.
    void deleteUserById(Long id);

    // Updates an existing user.
    User updateUser(Long id, User updatedUser);

    // Creates a new user.
    User createUser(User newUser);
}
