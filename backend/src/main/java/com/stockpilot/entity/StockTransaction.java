package com.stockpilot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionTypeEnum transactionType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "previous_available_quantity", nullable = false)
    @Builder.Default
    private Integer previousAvailableQuantity = 0;

    @Column(name = "new_available_quantity", nullable = false)
    @Builder.Default
    private Integer newAvailableQuantity = 0;

    @Column(name = "previous_reserved_quantity", nullable = false)
    @Builder.Default
    private Integer previousReservedQuantity = 0;

    @Column(name = "new_reserved_quantity", nullable = false)
    @Builder.Default
    private Integer newReservedQuantity = 0;

    @Column(name = "previous_damaged_quantity", nullable = false)
    @Builder.Default
    private Integer previousDamagedQuantity = 0;

    @Column(name = "new_damaged_quantity", nullable = false)
    @Builder.Default
    private Integer newDamagedQuantity = 0;

    private String reason;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
