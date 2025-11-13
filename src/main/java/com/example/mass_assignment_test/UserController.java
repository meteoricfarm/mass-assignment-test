package com.example.mass_assignment_test;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils; // 1. HtmlUtils 임포트

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
        
        // 2. [XSS 해결] DTO의 값을 HTML 인코딩하여 정화합니다.
        String safeUsername = HtmlUtils.htmlEscape(userDto.getUsername());
        String safeEmail = HtmlUtils.htmlEscape(userDto.getEmail());

        // 3. 정화된(안전한) 데이터로 엔티티를 업데이트합니다.
        currentUser.setUsername(safeUsername);
        currentUser.setEmail(safeEmail);

        // 이제 Coverity는 'safeUsername'이 정화 함수를 통과했음을 인지하고
        // 'save' 메소드로 전달되어도 XSS 경고를 발생시키지 않습니다.
        userRepository.save(currentUser);
        
        return "Safe update complete. Check H2 console for user: " + currentUser.getId();
    }
}