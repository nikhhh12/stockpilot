package com.stockpilot.repository;

import com.stockpilot.entity.Reservation;
import com.stockpilot.entity.ReservationStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {
    List<Reservation> findByProductIdAndWarehouseId(Long productId, Long warehouseId);
    List<Reservation> findByReferenceNumber(String referenceNumber);
    List<Reservation> findByStatus(ReservationStatusEnum status);
}
