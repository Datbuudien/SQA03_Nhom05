package com.example.accountservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;
import com.example.accountservice.model.OAuth2UserInfo;
import com.example.accountservice.repository.AccountRepository;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user: {}", ex.getMessage());
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        OAuth2UserInfo userInfo = new OAuth2UserInfo(oauth2User.getAttributes());

        String provider = userRequest.getClientRegistration().getRegistrationId(); // "google"

        // Tìm hoặc tạo account
        Optional<Account> accountOpt = accountRepository.findByEmailAndVisible(userInfo.getEmail(), 1);
        Account account;

        if (accountOpt.isEmpty()) {
            // Tạo account mới từ Google
            account = createAccountFromOAuth2(userInfo, provider);
            log.info("Created new account from OAuth2: {}", account.getEmail());
        } else {
            // Cập nhật thông tin nếu cần
            account = accountOpt.get();
            account = updateAccountFromOAuth2(account, userInfo, provider);
            log.info("Updated existing account from OAuth2: {}", account.getEmail());
        }

        return oauth2User;
    }

    private Account createAccountFromOAuth2(OAuth2UserInfo userInfo, String provider) {
        Account account = new Account();

        account.setEmail(userInfo.getEmail());
        account.setFirstName(userInfo.getGivenName() != null ? userInfo.getGivenName() : "");
        account.setLastName(userInfo.getFamilyName() != null ? userInfo.getFamilyName() : "");
        account.setGoogleId(userInfo.getSub());
        account.setPicture(userInfo.getPicture());
        account.setProvider(provider);
        account.setRole(Role.STUDENT); // Mặc định là STUDENT
        account.setVisible(1);

        // Tạo username từ email
        String username = userInfo.getEmail().split("@")[0];
        account.setUsername(ensureUniqueUsername(username));

        // OAuth2 không cần password
        account.setPassword(null);

        return accountRepository.save(account);
    }

    private Account updateAccountFromOAuth2(Account account, OAuth2UserInfo userInfo, String provider) {
        boolean needsUpdate = false;

        // Cập nhật Google ID nếu chưa có
        if (account.getGoogleId() == null && userInfo.getSub() != null) {
            account.setGoogleId(userInfo.getSub());
            needsUpdate = true;
        }

        // Cập nhật picture
        if (userInfo.getPicture() != null && !userInfo.getPicture().equals(account.getPicture())) {
            account.setPicture(userInfo.getPicture());
            needsUpdate = true;
        }

        // Cập nhật provider nếu chưa có
        if (account.getProvider() == null) {
            account.setProvider(provider);
            needsUpdate = true;
        }

        if (needsUpdate) {
            return accountRepository.save(account);
        }

        return account;
    }

    private String ensureUniqueUsername(String baseUsername) {
        String username = baseUsername;
        int counter = 1;

        while (accountRepository.existsByUsernameAndVisible(username, 1)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
}