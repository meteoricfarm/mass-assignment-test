package com.example.mass_assignment_test;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        User testUser = new User();
        testUser.setUsername("normalUser");
        testUser.setEmail("normal@example.com");
        testUser.setRole("USER"); // 초기 권한
        userRepository.save(testUser);

        System.out.println("--- 테스트 유저 생성 완료 (ID: 1, Role: USER) ---");
    }
}