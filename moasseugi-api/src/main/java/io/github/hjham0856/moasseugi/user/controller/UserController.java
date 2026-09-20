package io.github.hjham0856.moasseugi.user.controller;

import io.github.hjham0856.moasseugi.user.dto.UserResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public abstract class UserController {
    @GetMapping("/api/users/me")
    public abstract UserResponse getCurrentUser();

    // TODO: JWT 인증 사용자 조회 구현
}
