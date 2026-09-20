package io.github.hjham0856.moasseugi.user.controller;

import io.github.hjham0856.moasseugi.user.dto.LoginRequest;
import io.github.hjham0856.moasseugi.user.dto.LoginResponse;
import io.github.hjham0856.moasseugi.user.dto.SignupRequest;
import io.github.hjham0856.moasseugi.user.dto.UserResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public abstract class AuthController {
    @PostMapping("/api/auth/signup")
    public abstract UserResponse signup(SignupRequest request);

    @PostMapping("/api/auth/login")
    public abstract LoginResponse login(LoginRequest request);

    // TODO: 요청 본문 바인딩, 입력 검증, 비밀번호 암호화 및 JWT 인증 구현
}
