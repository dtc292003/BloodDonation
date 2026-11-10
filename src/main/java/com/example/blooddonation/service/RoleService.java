package com.example.blooddonation.service;

import com.example.blooddonation.entity.Role;
import com.example.blooddonation.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleService {
    private final RoleRepository roleRepo;

    // Find All
    public List<Role> getAllRoles(){
        return roleRepo.findAll();
    }

    // Find By ID

    public Role getById(Integer id){
        return roleRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Role với ID: " +id));
    }

    //Create
    public Role createRole(Role role){
        if(roleRepo.existsByRoleName(role.getRoleName())){
            throw new RuntimeException("Vai trò" + role.getRoleName() +" đã tồn tại");
        }
        return  roleRepo.save(role);
    }

    //Update

    public Role updateRole(Integer id, Role roleInfo){
        Role role = getById(id);
        role.setRoleName(roleInfo.getRoleName());
        role.setDescription(roleInfo.getDescription());
        return roleRepo.save(role);
    }

     //Delete

    public void deleteRole(Integer id){
        if(!roleRepo.existsById(id)){
            throw new RuntimeException("Không tìm thấy Role với ID: " +id);
        }
        roleRepo.deleteById(id);
    }

}
