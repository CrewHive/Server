package com.pat.crewhive.service;

import com.pat.crewhive.dto.AuthRequestDTO;
import com.pat.crewhive.dto.RegistrationDTO;
import com.pat.crewhive.dto.UserDTO;
import com.pat.crewhive.model.time_management.entity.event.PersonalEvent;
import com.pat.crewhive.model.user.entity.role.Role;
import com.pat.crewhive.model.user.entity.role.UserRole;
import com.pat.crewhive.repository.RoleRepository;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.repository.CompanyRepository;
import com.pat.crewhive.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService {

    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;

    private CompanyRepository companyRepository;

    private RoleRepository roleRepository;

    public UserService() {}

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, CompanyRepository companyRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.companyRepository = companyRepository;
        this.roleRepository = roleRepository;
    }

    public User getUserById(Long id) {

        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("User found with id: {}", user.getUserId());

        return user;
    }

    public User getUserByUsername(String username) {

        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        log.info("User found with username: {}", user.getUsername());

        return user;
    }

    public User getUserByIdIdentity(Long id) {

        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("User found with id: {}", user.getUserId());

        return user;
    }

    public boolean validatePassword(AuthRequestDTO dto) {

        User user = userRepository.findByUsername(dto.getUsername()).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("User found with id: {}", user.getUserId());

        boolean match = passwordEncoder.matches(dto.getPassword(), user.getPassword());

        log.info("Password match: {}", match);

        return match;
    }

    //todo: assolutamente da rivedere
    public void register(RegistrationDTO rDTO) {

        Company companyName = companyRepository.findByName(rDTO.getCompanyName()).orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        log.info("Company found with name: {}", companyName.getName());

        String password = passwordEncoder.encode(rDTO.getPassword());

        if (userRepository.existsByUsername(rDTO.getUsername())) {

            log.error("Username already exists: {}", rDTO.getUsername());
            throw new ResourceAlreadyExistsException("Username already exists");
        }

        User user = new User(rDTO.getUsername(), rDTO.getEmail(), password, companyName, rDTO.getContract());
        Role role = roleRepository.findByRoleName(rDTO.getRole()).orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        UserRole userRole = new UserRole(user, role);
        user.setRole(userRole);

        log.info("User created with id: {}", user.getUserId());

        userRepository.save(user);
    }
}
