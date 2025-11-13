package com.example.massassignmenttest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * [1. 취약한 컨트롤러]
     * @ModelAttribute User user
     * 이 부분이 HTTP 요청의 모든 파라미터를 User 엔티티에 직접 바인딩합니다.
     */
    @PostMapping("/profile/update_vulnerable")
    @ResponseBody // HTML 템플릿 없이 결과를 바로 반환 (테스트용)
    public String updateUserVulnerable(@ModelAttribute User user) {
        // 공격자가 보낸 'id'가 포함된 user 객체를 그대로 저장
        userRepository.save(user);
        return "Vulnerable update complete. Check H2 console for user: " + user.getId();
    }

    /**
     * [2. 안전한 컨트롤러]
     * @ModelAttribute UserUpdateDto userDto
     * 'role'이 없는 DTO로 요청을 받습니다.
     */
    @PostMapping("/profile/update_safe/{id}")
    @ResponseBody
    public String updateUserSafe(@PathVariable Long id, @ModelAttribute UserUpdateDto userDto) {
        
        // 1. DB에서 반드시 원본 엔티티를 조회합니다.
        User currentUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        // 2. DTO에 허용된 필드만 '수동으로' 매핑합니다.
        currentUser.setUsername(userDto.getUsername());
        currentUser.setEmail(userDto.getEmail());

        // 3. 'role' 필드는 절대 건드리지 않고 저장합니다.
        userRepository.save(currentUser);
        
        return "Safe update complete. Check H2 console for user: " + currentUser.getId();
    }
}