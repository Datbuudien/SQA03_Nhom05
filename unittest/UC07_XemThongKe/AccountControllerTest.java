/**
 * ============================================================================
 * UNIT TEST CLASS: AccountControllerTest
 * ============================================================================
 * Purpose: Unit tests for AccountController (account count via search)
 * - Focus: /account/search endpoint used for statistics
 * - Uses MockMvc with mocked AccountService
 * 
 * Author: QA Team
 * Date: 19/4/2026
 * Version: 1.0
 * ============================================================================
 */

package com.example.accountservice.controller;

import com.example.accountservice.annotation.RoleCheckAspect;
import com.example.accountservice.model.Account;
import com.example.accountservice.service.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AccountController.class, properties = {
        "spring.security.oauth2.client.registration.google.client-id=dummy",
        "spring.security.oauth2.client.registration.google.client-secret=dummy"
})
@ContextConfiguration(classes = AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AccountController - Unit Tests")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private RoleCheckAspect roleCheckAspect;

    /**
     * Test Case: TK035
     * Scenario: Search accounts returns page with totalElements
     * Expected: 200 OK and totalElements in response
     */
    @Test
    @DisplayName("TK035: /account/search returns totalElements")
    void testSearchAccounts_ShouldReturnTotalElements() throws Exception {
        Account account = new Account();
        account.setId(1L);
        account.setUsername("user1");

        Page<Account> page = new PageImpl<>(
                Collections.singletonList(account),
                PageRequest.of(0, 1),
                12);

        when(accountService.universalSearch(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/account/search")
                .param("size", "1")
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(12)));

        verify(accountService, times(1)).universalSearch(any(), any(Pageable.class));
    }

    /**
     * Test Case: TK036
     * Scenario: totalElements off-by-one (intentional fail)
     * Expected: Fail to document pagination count mismatch
     */
    @Test
    @DisplayName("TK036: Fail - Off-by-one totalElements")
    void testSearchAccounts_ShouldFailOnWrongTotalElements() throws Exception {
        Account account = new Account();
        account.setId(1L);
        account.setUsername("user1");

        Page<Account> page = new PageImpl<>(
                Collections.singletonList(account),
                PageRequest.of(0, 1),
                11);

        when(accountService.universalSearch(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/account/search")
                .param("size", "1")
                .param("page", "0")
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(12)));
    }
}