package com.example.blooddonation.controller;

import com.example.blooddonation.entity.User;
import com.example.blooddonation.enums.UserStatus;
import com.example.blooddonation.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    //Get All User
    @GetMapping
    public ResponseEntity<List<User>> getAll(){
        return ResponseEntity.ok(userService.getAllUser());
    }

    //Get By Id User
    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable Integer id){
        return ResponseEntity.ok(userService.getById(id));
    }

    //Create User
    @PostMapping
    public ResponseEntity<User> create(@RequestBody User user){
        return ResponseEntity.ok(userService.createUser(user));
    }

    //Update User
    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Integer id, @RequestBody User user){
        return ResponseEntity.ok(userService.updateUser(id,user));
    }

    //Delete User
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    //Update UserStatus
    @PutMapping("/{id}/status")
    public ResponseEntity<User> updateStatus(@PathVariable Integer id, @RequestParam UserStatus status){
        return ResponseEntity.ok(userService.updateUserStatus(id,status));
    }

    //Find By Email
    @GetMapping("/search")
    public ResponseEntity<User> getByEmail(@RequestParam String email){
        return ResponseEntity.ok(userService.getByEmail(email));
    }
}
