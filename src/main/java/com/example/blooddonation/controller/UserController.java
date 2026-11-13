package com.example.blooddonation.controller;

import com.example.blooddonation.dto.UserUpdateRequest;
import com.example.blooddonation.entity.User;
import com.example.blooddonation.service.UserService;
import com.example.blooddonation.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    //Get All User
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll(){
        return ResponseEntity.ok(userService.getAllUser());
    }

    //Get By Id User
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Integer id){
        return ResponseEntity.ok(userService.getUserResponse(id));
    }

    //Create User
    @PostMapping
    public ResponseEntity<User> create(@RequestBody User user){
        return ResponseEntity.ok(userService.createUser(user));
    }

    //Update User
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Integer id, @Valid @RequestBody UserUpdateRequest request){
        return ResponseEntity.ok(userService.updateUser(id,request));
    }

    //Delete User
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    //Find By Email
    @GetMapping("/search")
    public ResponseEntity<UserResponse> SearchByEmail(@RequestParam String email){
        return ResponseEntity.ok(userService.getUserResponseByEmail(email));
    }
}
