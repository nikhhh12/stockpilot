package com.stockpilot.service.impl;

import com.stockpilot.dto.request.WarehouseRequest;
import com.stockpilot.dto.response.PagedResponse;
import com.stockpilot.dto.response.WarehouseResponse;
import com.stockpilot.entity.Inventory;
import com.stockpilot.entity.Warehouse;
import com.stockpilot.exception.DuplicateResourceException;
import com.stockpilot.exception.ResourceNotFoundException;
import com.stockpilot.mapper.WarehouseMapper;
import com.stockpilot.repository.InventoryRepository;
import com.stockpilot.repository.WarehouseRepository;
import com.stockpilot.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseMapper warehouseMapper;

    @Override
    @Transactional
    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (warehouseRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Warehouse code '" + code + "' already exists");
        }

        Warehouse warehouse = Warehouse.builder()
                .name(request.getName().trim())
                .code(code)
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .active(request.isActive())
                .build();

        Warehouse saved = warehouseRepository.save(warehouse);
        return warehouseMapper.toResponse(saved, 0, 0, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseById(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));
        return buildWarehouseResponse(warehouse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseResponse> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(this::buildWarehouseResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<WarehouseResponse> getWarehouses(String search, Boolean active, Pageable pageable) {
        Page<Warehouse> page = warehouseRepository.findAll((root, query, cb) -> {
            var predicate = cb.conjunction();
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("city")), pattern)
                ));
            }
            if (active != null) {
                predicate = cb.and(predicate, cb.equal(root.get("active"), active));
            }
            return predicate;
        }, pageable);

        Page<WarehouseResponse> mappedPage = page.map(this::buildWarehouseResponse);
        return PagedResponse.from(mappedPage);
    }

    @Override
    @Transactional
    public WarehouseResponse updateWarehouse(Long id, WarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));

        String code = request.getCode().trim().toUpperCase();
        if (!warehouse.getCode().equalsIgnoreCase(code) && warehouseRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Warehouse code '" + code + "' already exists");
        }

        warehouse.setName(request.getName().trim());
        warehouse.setCode(code);
        warehouse.setAddress(request.getAddress());
        warehouse.setCity(request.getCity());
        warehouse.setState(request.getState());
        warehouse.setCountry(request.getCountry());
        warehouse.setActive(request.isActive());

        Warehouse saved = warehouseRepository.save(warehouse);
        return buildWarehouseResponse(saved);
    }

    @Override
    @Transactional
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));

        List<Inventory> inventories = inventoryRepository.findByWarehouseId(id);
        if (!inventories.isEmpty()) {
            warehouse.setActive(false);
            warehouseRepository.save(warehouse);
        } else {
            warehouseRepository.delete(warehouse);
        }
    }

    private WarehouseResponse buildWarehouseResponse(Warehouse warehouse) {
        List<Inventory> inventories = inventoryRepository.findByWarehouseId(warehouse.getId());
        long avail = inventories.stream().mapToLong(i -> i.getAvailableQuantity() != null ? i.getAvailableQuantity() : 0).sum();
        long res = inventories.stream().mapToLong(i -> i.getReservedQuantity() != null ? i.getReservedQuantity() : 0).sum();
        long dam = inventories.stream().mapToLong(i -> i.getDamagedQuantity() != null ? i.getDamagedQuantity() : 0).sum();

        return warehouseMapper.toResponse(warehouse, avail, res, dam);
    }
}
