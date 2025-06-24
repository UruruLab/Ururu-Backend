package com.ururulab.ururu.global.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 정적 리소스 라우팅 컨트롤러 (개발 환경 전용).
 */
@Slf4j
@Controller
@Profile("dev")
public final class StaticResourceController {

    @GetMapping("/")
    public String index() {
        log.debug("Main page requested");
        return "forward:/index.html";
    }

    @GetMapping("/login")
    public String login() {
        log.debug("Login page requested");
        return "forward:/index.html";
    }
}