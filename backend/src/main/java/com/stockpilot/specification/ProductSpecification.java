package com.stockpilot.specification;

import com.stockpilot.entity.Product;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class ProductSpecification {

    public static Specification<Product> filterProducts(String search, Long categoryId, Long supplierId, Boolean active) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                var nameLike = cb.like(cb.lower(root.get("name")), pattern);
                var skuLike = cb.like(cb.lower(root.get("sku")), pattern);
                var descLike = cb.like(cb.lower(root.get("description")), pattern);
                predicate = cb.and(predicate, cb.or(nameLike, skuLike, descLike));
            }

            if (categoryId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("category").get("id"), categoryId));
            }

            if (supplierId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("supplier").get("id"), supplierId));
            }

            if (active != null) {
                predicate = cb.and(predicate, cb.equal(root.get("active"), active));
            }

            return predicate;
        };
    }
}
