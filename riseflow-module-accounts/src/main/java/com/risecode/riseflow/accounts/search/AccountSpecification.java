package com.risecode.riseflow.accounts.search;

import com.risecode.riseflow.accounts.domain.Account;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class AccountSpecification {

    private AccountSpecification() {
    }

    public static Specification<Account> withTenant(UUID tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<Account> customFieldEquals(String fieldName, String value) {
        return (root, query, cb) -> {
            Expression<String> jsonValue = jsonExtractText(cb, root, fieldName);
            return cb.equal(jsonValue, value);
        };
    }

    public static Specification<Account> customFieldContains(String fieldName, String value) {
        return (root, query, cb) -> {
            Expression<String> jsonValue = jsonExtractText(cb, root, fieldName);
            return cb.like(cb.lower(jsonValue), "%" + value.toLowerCase() + "%");
        };
    }

    public static Specification<Account> customFieldGreaterThan(String fieldName, String value) {
        return (root, query, cb) -> {
            Expression<String> jsonValue = jsonExtractText(cb, root, fieldName);
            Expression<Double> numericValue = jsonValue.as(Double.class);
            return cb.greaterThan(numericValue, Double.parseDouble(value));
        };
    }

    public static Specification<Account> customFieldLessThan(String fieldName, String value) {
        return (root, query, cb) -> {
            Expression<String> jsonValue = jsonExtractText(cb, root, fieldName);
            Expression<Double> numericValue = jsonValue.as(Double.class);
            return cb.lessThan(numericValue, Double.parseDouble(value));
        };
    }

    public static Specification<Account> customFieldExists(String fieldName) {
        return (root, query, cb) -> cb.isNotNull(jsonExtractText(cb, root, fieldName));
    }

    public static Specification<Account> fromCriteria(List<SearchCriteria> criteria, UUID tenantId) {
        List<Specification<Account>> specs = new ArrayList<>();
        specs.add(withTenant(tenantId));

        for (SearchCriteria c : criteria) {
            Specification<Account> spec = switch (c.operator()) {
                case EQUALS -> customFieldEquals(c.fieldName(), c.value());
                case CONTAINS -> customFieldContains(c.fieldName(), c.value());
                case GT -> customFieldGreaterThan(c.fieldName(), c.value());
                case LT -> customFieldLessThan(c.fieldName(), c.value());
                case EXISTS -> customFieldExists(c.fieldName());
            };
            specs.add(spec);
        }

        return specs.stream()
                .reduce(Specification::and)
                .orElse(withTenant(tenantId));
    }

    private static Expression<String> jsonExtractText(CriteriaBuilder cb, Root<Account> root, String fieldName) {
        return cb.function("jsonb_extract_path_text", String.class,
                root.get("customFields"),
                cb.literal(fieldName));
    }
}
