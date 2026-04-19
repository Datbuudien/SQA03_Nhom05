package com.example.react_flow_be.specification;

import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.entity.Diagram;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;
import java.time.LocalDateTime;

public class DiagramSpecification {

    /**
     * Filter by deleted status
     */
    public static Specification<Diagram> isDeleted(Boolean isDeleted) {
        return (root, query, cb) -> {
            if (isDeleted == null)
                return null;
            return cb.equal(root.get("isDeleted"), isDeleted);
        };
    }

    /**
     * Filter by name starting with letter
     */
    public static Specification<Diagram> nameStartsWith(String letter) {
        return (root, query, cb) -> {
            if (letter == null || letter.isEmpty())
                return null;
            return cb.like(cb.lower(root.get("name")), letter.toLowerCase() + "%");
        };
    }

    /**
     * Search by name or owner username
     */
    public static Specification<Diagram> searchQuery(String searchQuery) {
        return (root, query, cb) -> {
            if (searchQuery == null || searchQuery.trim().isEmpty())
                return null;

            String likePattern = "%" + searchQuery.toLowerCase() + "%";

            // Join với Collaboration để search owner
            Join<Diagram, Collaboration> collaborationJoin = root.join("collaboration", JoinType.LEFT);

            Predicate namePredicate = cb.like(cb.lower(root.get("name")), likePattern);
            Predicate ownerPredicate = cb.and(
                    cb.like(cb.lower(collaborationJoin.get("username")), likePattern),
                    cb.equal(collaborationJoin.get("type"), Collaboration.CollaborationType.OWNER));

            return cb.or(namePredicate, ownerPredicate);
        };
    }

    /**
     * Filter by owner (me or team)
     */
    public static Specification<Diagram> ownerFilter(String ownerFilter, String currentUsername) {
        return (root, query, cb) -> {
            if (ownerFilter == null || ownerFilter.isEmpty() || currentUsername == null)
                return null;

            Join<Diagram, Collaboration> collaborationJoin = root.join("collaboration", JoinType.INNER);

            if ("me".equalsIgnoreCase(ownerFilter)) {
                // Chỉ lấy diagram mà user là owner
                return cb.and(
                        cb.equal(collaborationJoin.get("username"), currentUsername),
                        cb.equal(collaborationJoin.get("type"), Collaboration.CollaborationType.OWNER));
            } else if ("team".equalsIgnoreCase(ownerFilter)) {
                // Lấy diagram có collaboration (nhiều hơn 1 người)
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<Collaboration> subRoot = subquery.from(Collaboration.class);
                subquery.select(cb.count(subRoot.get("id")))
                        .where(cb.equal(subRoot.get("diagram"), root));

                return cb.greaterThan(subquery, 1L);
            }

            return null;
        };
    }

    /**
     * Filter by shared with me (not owner)
     */
    public static Specification<Diagram> sharedWithMe(Boolean sharedWithMe, String currentUsername) {
        return (root, query, cb) -> {
            if (sharedWithMe == null || !sharedWithMe || currentUsername == null)
                return null;

            Join<Diagram, Collaboration> collaborationJoin = root.join("collaboration", JoinType.INNER);

            return cb.and(
                    cb.equal(collaborationJoin.get("username"), currentUsername),
                    cb.equal(collaborationJoin.get("type"), Collaboration.CollaborationType.PARTICIPANTS),
                    cb.equal(collaborationJoin.get("isActive"), true));
        };
    }

    /**
     * Filter by date range
     */
    public static Specification<Diagram> dateRange(String dateRange) {
        return (root, query, cb) -> {
            if (dateRange == null || "alltime".equalsIgnoreCase(dateRange))
                return null;

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate = null;

            switch (dateRange.toLowerCase()) {
                case "today":
                    startDate = now.toLocalDate().atStartOfDay();
                    break;
                case "last7days":
                    startDate = now.minusDays(7);
                    break;
                case "last30days":
                    startDate = now.minusDays(30);
                    break;
                default:
                    return null;
            }

            return cb.greaterThanOrEqualTo(root.get("updatedAt"), startDate);
        };
    }

    /**
     * User has access to diagram (owner or participant)
     */
    public static Specification<Diagram> userHasAccess(String username) {
        return (root, query, cb) -> {
            if (username == null)
                return null;

            Join<Diagram, Collaboration> collaborationJoin = root.join("collaboration", JoinType.INNER);

            return cb.and(
                    cb.equal(collaborationJoin.get("username"), username),
                    cb.equal(collaborationJoin.get("isActive"), true));
        };
    }

    /**
     * Cursor-based pagination (for infinite scroll)
     */
    public static Specification<Diagram> afterId(Long lastDiagramId) {
        return (root, query, cb) -> {
            if (lastDiagramId == null)
                return null;
            return cb.lessThan(root.get("id"), lastDiagramId);
        };
    }
}