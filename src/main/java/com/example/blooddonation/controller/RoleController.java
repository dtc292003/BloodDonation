package com.example.blooddonation.controller;

import com.example.blooddonation.entity.Role;
import com.example.blooddonation.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    //Get All Role

    @GetMapping
    public ResponseEntity<List<Role>> getAll(){
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    //GetById Role

    @GetMapping("/{id}")
    public ResponseEntity<Role> getById(@PathVariable Integer id){
        return ResponseEntity.ok(roleService.getById(id));
    }

    // Create Role

    @PostMapping
    public ResponseEntity<Role> create(@RequestBody Role role){
        return ResponseEntity.ok(roleService.createRole(role));
    }

    // Update Role

    @PutMapping("/{id}")
    public ResponseEntity<Role> update(@PathVariable Integer id, @RequestBody Role role){
        return ResponseEntity.ok(roleService.updateRole(id, role));
    }

    //Delete Role

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
