package com.example.accountservice.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.example.accountservice.dto.AccountSearchDTO;
import com.example.accountservice.enums.Role;
import com.example.accountservice.model.Account;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Component
public class AccountSpecification {

    public static Specification<Account> isVisible() {
        return (root, query, cb) -> cb.equal(root.get("visible"), 1);
    }

    public static Specification<Account> hasRole(Role role) {
        return (root, query, cb) -> role == null ? cb.conjunction() : cb.equal(root.get("role"), role);
    }

    public static Specification<Account> keywordInFields(String keyword, List<String> searchFields) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }

            String likePattern = "%" + keyword.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            List<String> fieldsToSearch = (searchFields == null || searchFields.isEmpty())
                    ? List.of("username", "firstName", "lastName", "fullName", "phoneNumber", "email", "cccd")
                    : searchFields;

            for (String field : fieldsToSearch) {
                switch (field) {
                    case "username":
                        predicates.add(cb.like(cb.lower(root.get("username")), likePattern));
                        break;
                    case "firstName":
                        predicates.add(cb.like(cb.lower(root.get("firstName")), likePattern));
                        break;
                    case "lastName":
                        predicates.add(cb.like(cb.lower(root.get("lastName")), likePattern));
                        break;
                    case "fullName":
                        Expression<String> fullName1 = cb.concat(cb.concat(root.get("firstName"), " "),
                                root.get("lastName"));
                        Expression<String> fullName2 = cb.concat(cb.concat(root.get("lastName"), " "),
                                root.get("firstName"));
                        predicates.add(cb.or(
                                cb.like(cb.lower(fullName1), likePattern),
                                cb.like(cb.lower(fullName2), likePattern)));
                        break;
                    case "phoneNumber":
                        predicates.add(cb.and(
                                cb.isNotNull(root.get("phoneNumber")),
                                cb.like(root.get("phoneNumber"), likePattern)));
                        break;
                    case "email":
                        predicates.add(cb.and(
                                cb.isNotNull(root.get("email")),
                                cb.like(cb.lower(root.get("email")), likePattern)));
                        break;
                    case "cccd":
                        predicates.add(cb.and(
                                cb.isNotNull(root.get("cccd")),
                                cb.like(root.get("cccd"), likePattern)));
                        break;
                }
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Account> build(AccountSearchDTO searchDTO) {
        return Specification.where(isVisible())
                .and(hasRole(searchDTO.getRole()))
                .and(keywordInFields(searchDTO.getKeyword(), searchDTO.getSearchFields()));
    }
}
