package com.stockpilot.service.impl;

import com.stockpilot.dto.request.SupplierRequest;
import com.stockpilot.dto.response.PagedResponse;
import com.stockpilot.dto.response.SupplierResponse;
import com.stockpilot.entity.PurchaseOrder;
import com.stockpilot.entity.Supplier;
import com.stockpilot.exception.ResourceNotFoundException;
import com.stockpilot.mapper.SupplierMapper;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.PurchaseOrderRepository;
import com.stockpilot.repository.SupplierRepository;
import com.stockpilot.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierMapper supplierMapper;

    @Override
    @Transactional
    public SupplierResponse createSupplier(SupplierRequest request) {
        Supplier supplier = Supplier.builder()
                .companyName(request.getCompanyName().trim())
                .contactPerson(request.getContactPerson())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .taxIdentifier(request.getTaxIdentifier())
                .defaultLeadTimeDays(request.getDefaultLeadTimeDays() != null && request.getDefaultLeadTimeDays() > 0 ? request.getDefaultLeadTimeDays() : 7)
                .active(request.isActive())
                .build();

        Supplier saved = supplierRepository.save(supplier);
        return supplierMapper.toResponse(saved, 0, 0, BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));
        return buildSupplierResponse(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponse> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .map(this::buildSupplierResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SupplierResponse> getSuppliers(String search, Boolean active, Pageable pageable) {
        Page<Supplier> page = supplierRepository.findAll((root, query, cb) -> {
            var predicate = cb.conjunction();
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("companyName")), pattern),
                        cb.like(cb.lower(root.get("contactPerson")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)
                ));
            }
            if (active != null) {
                predicate = cb.and(predicate, cb.equal(root.get("active"), active));
            }
            return predicate;
        }, pageable);

        Page<SupplierResponse> mappedPage = page.map(this::buildSupplierResponse);
        return PagedResponse.from(mappedPage);
    }

    @Override
    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));

        supplier.setCompanyName(request.getCompanyName().trim());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());
        supplier.setTaxIdentifier(request.getTaxIdentifier());
        if (request.getDefaultLeadTimeDays() != null && request.getDefaultLeadTimeDays() > 0) {
            supplier.setDefaultLeadTimeDays(request.getDefaultLeadTimeDays());
        }
        supplier.setActive(request.isActive());

        Supplier saved = supplierRepository.save(supplier);
        return buildSupplierResponse(saved);
    }

    @Override
    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));

        List<PurchaseOrder> pos = purchaseOrderRepository.findBySupplierId(id);
        if (!pos.isEmpty()) {
            supplier.setActive(false);
            supplierRepository.save(supplier);
        } else {
            supplierRepository.delete(supplier);
        }
    }

    private SupplierResponse buildSupplierResponse(Supplier supplier) {
        long productCount = productRepository.findAll((root, query, cb) ->
                cb.equal(root.get("supplier").get("id"), supplier.getId())).size();

        List<PurchaseOrder> pos = purchaseOrderRepository.findBySupplierId(supplier.getId());
        long poCount = pos.size();
        BigDecimal totalValue = pos.stream()
                .map(PurchaseOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return supplierMapper.toResponse(supplier, productCount, poCount, totalValue);
    }
}
