package com.example.accountservice.config;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.accountservice.model.Account;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.service.RedisTokenService;
import com.example.accountservice.util.JwtUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTokenService redisTokenService;

    @Autowired
    private AccountRepository accountRepository;

    @Value("${oauth2.redirect-uri:http://localhost:5173}")
    private String frontendUrl;

    @Value("${https.config}")
    private boolean https;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        if (response.isCommitted()) {
            log.debug("Response has already been committed");
            return;
        }

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");

        try {
            // Tìm account theo email
            Optional<Account> accountOpt = accountRepository.findByEmailAndVisible(email, 1);

            if (accountOpt.isEmpty()) {
                log.error("Account not found for email: {}", email);
                response.sendRedirect(frontendUrl + "/login?error=account_not_found");
                return;
            }

            Account account = accountOpt.get();

            // Tạo JWT token
            String token = jwtUtil.generateToken(account);
            String jwtId = jwtUtil.getJwtIdFromToken(token);

            // Lưu token info vào Redis
            redisTokenService.saveTokenInfo(jwtId, account);

            // Set JWT cookie
            Cookie jwtCookie = new Cookie("jwt", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(https);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(24 * 60 * 60); // 24 hours
            jwtCookie.setAttribute("SameSite", https ? "None" : "Lax");
            response.addCookie(jwtCookie);

            // Redirect về frontend với token
            String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                    .queryParam("token", token)
                    .build().toUriString();

            log.info("OAuth2 login successful for user: {}", account.getEmail());
            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            log.error("Error during OAuth2 success handling: {}", e.getMessage());
            response.sendRedirect(frontendUrl + "/login?error=server_error");
        }
    }
}