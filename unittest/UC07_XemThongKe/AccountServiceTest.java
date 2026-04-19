/**
 * ============================================================================
 * UNIT TEST CLASS: AccountServiceTest
 * ============================================================================
 * Purpose: Unit tests for AccountService (account count via search)
 * - Focus: universalSearch() behavior for statistics usage
 * - Ensures branch coverage for empty and non-empty results
 * 
 * Author: QA Team
 * Date: 19/4/2026
 * Version: 1.0
 * ============================================================================
 */

package com.example.accountservice.service;

import com.example.accountservice.dto.AccountSearchDTO;
import com.example.accountservice.model.Account;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.util.UsernameGenerator;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService - Unit Tests")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UsernameGenerator usernameGenerator;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private AccountService accountService;

    /**
     * Test Case: TK032
     * Scenario: universalSearch returns non-empty page
     * Expected: Page returned with total elements
     */
    @Test
    @DisplayName("TK032: universalSearch returns page when data exists")
    void testUniversalSearch_WhenResultsExist_ShouldReturnPage() {
        AccountSearchDTO searchDTO = new AccountSearchDTO();
        searchDTO.setKeyword("user");
        searchDTO.setPositionIds(new java.util.ArrayList<>(Arrays.asList(1L, -1L, null)));
        searchDTO.setSearchFields(new java.util.ArrayList<>(Arrays.asList("username", "invalid")));

        Pageable pageable = PageRequest.of(0, 1, Sort.by("id"));

        Account account = new Account();
        account.setId(1L);
        account.setUsername("user1");

        Page<Account> page = new PageImpl<>(
                Collections.singletonList(account),
                pageable,
                5);

        when(accountRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<Account> result = accountService.universalSearch(searchDTO, pageable);

        assertEquals(5, result.getTotalElements(), "Total elements should match");
        verify(accountRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    /**
     * Test Case: TK033
     * Scenario: universalSearch returns empty page
     * Expected: EntityNotFoundException thrown
     */
    @Test
    @DisplayName("TK033: universalSearch throws when no data")
    void testUniversalSearch_WhenNoResults_ShouldThrowEntityNotFound() {
        AccountSearchDTO searchDTO = new AccountSearchDTO();
        Pageable pageable = PageRequest.of(0, 1, Sort.by("id"));

        Page<Account> emptyPage = Page.empty(pageable);

        when(accountRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        assertThrows(EntityNotFoundException.class,
                () -> accountService.universalSearch(searchDTO, pageable),
                "Should throw EntityNotFoundException when no results");

        verify(accountRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    /**
     * Test Case: TK034
     * Scenario: Empty results should return empty page (intentional fail)
     * Expected: Fail to document thrown exception instead of empty page
     */
    @Test
    @DisplayName("TK034: Fail - Expect empty page instead of exception")
    void testUniversalSearch_WhenNoResults_ShouldFailExpectation() {
        AccountSearchDTO searchDTO = new AccountSearchDTO();
        Pageable pageable = PageRequest.of(0, 1, Sort.by("id"));

        Page<Account> emptyPage = Page.empty(pageable);

        when(accountRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<Account> result = accountService.universalSearch(searchDTO, pageable);

        assertEquals(0, result.getTotalElements(),
                "Intentional fail: expected empty page when no results");
    }
}