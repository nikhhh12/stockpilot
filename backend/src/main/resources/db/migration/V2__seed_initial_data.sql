-- Flyway Migration V2: Seed Realistic Development Data for StockPilot AI

-- BCrypt hash for 'password123': $2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxGQrvkWGYtC
INSERT INTO users (id, name, email, password_hash, role, enabled) VALUES
(1, 'System Administrator', 'admin@stockpilot.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'ADMIN', TRUE),
(2, 'Sarah Jenkins', 'manager@stockpilot.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'INVENTORY_MANAGER', TRUE),
(3, 'Alex Vance', 'staff@stockpilot.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'STAFF', TRUE);

-- Categories
INSERT INTO categories (id, name, description, active) VALUES
(1, 'Laptops & Workstations', 'Enterprise laptops, desktop computers, and portable workstations.', TRUE),
(2, 'Computer Peripherals', 'Keyboards, mice, monitors, headsets, and docking stations.', TRUE),
(3, 'Networking Gear', 'Routers, switches, access points, and server rack accessories.', TRUE),
(4, 'Storage Devices', 'Solid-state drives, external HDDs, NAS units, and memory modules.', TRUE),
(5, 'Office Ergonomics', 'Ergonomic chairs, standing desks, monitor arms, and accessories.', TRUE);

-- Suppliers
INSERT INTO suppliers (id, company_name, contact_person, email, phone, address, tax_identifier, default_lead_time_days, active) VALUES
(1, 'TechPulse Global Inc.', 'David Miller', 'supply@techpulse.io', '+1-555-0192', '100 Silicon Ave, San Jose, CA', 'TAX-US-99182', 5, TRUE),
(2, 'Nexus Hardware Distribution', 'Priya Sharma', 'orders@nexushw.com', '+91-80-491200', '42 Electronic City Phase 1, Bangalore', 'GST-IN-29AAA12', 7, TRUE),
(3, 'Apex Industrial Component Co.', 'Marcus Vance', 'sales@apexind.de', '+49-89-123456', 'Industriestrasse 15, Munich', 'DE-98127391', 12, TRUE),
(4, 'Horizon Logistics & Supplies', 'Kenji Sato', 'contact@horizon-logistics.jp', '+81-3-555123', '2-1-1 Otemachi, Chiyoda-ku, Tokyo', 'JP-77182910', 10, TRUE),
(5, 'Premier Ergonomics Solutions', 'Elena Rostova', 'corporate@premierergo.eu', '+44-20-794600', '12 Oxford Street, London', 'UK-GB881920', 8, TRUE);

-- Warehouses
INSERT INTO warehouses (id, name, code, address, city, state, country, active) VALUES
(1, 'Bangalore Central Hub', 'BLR-01', 'Plot 15, Outer Ring Road, Bellandur', 'Bangalore', 'Karnataka', 'India', TRUE),
(2, 'Hyderabad Fulfillment Center', 'HYD-01', 'Survey 88, HITEC City Phase 2', 'Hyderabad', 'Telangana', 'India', TRUE),
(3, 'Mumbai Western Logistics', 'BOM-01', 'Warehouse Complex B, Bhiwandi', 'Mumbai', 'Maharashtra', 'India', TRUE);

-- 20 Realistic Products
INSERT INTO products (id, sku, name, description, category_id, supplier_id, cost_price, selling_price, unit, reorder_level, reorder_quantity, active) VALUES
(1, 'PROD-LAP-001', 'Dell Latitude 5540 i7', '15.6" Full HD, Intel Core i7 13th Gen, 16GB RAM, 512GB SSD', 1, 1, 950.00, 1250.00, 'unit', 15, 30, TRUE),
(2, 'PROD-LAP-002', 'MacBook Pro 14" M3', 'Apple M3 chip, 8-core CPU, 10-core GPU, 18GB Unified Memory, 512GB SSD', 1, 1, 1750.00, 1999.00, 'unit', 10, 20, TRUE),
(3, 'PROD-LAP-003', 'Lenovo ThinkPad X1 Carbon Gen 11', '14" WUXGA Touch, Intel i7-1365U, 32GB RAM, 1TB SSD', 1, 2, 1400.00, 1799.00, 'unit', 12, 25, TRUE),
(4, 'PROD-PER-001', 'Logitech MX Master 3S Wireless Mouse', '8K DPI sensor, quiet clicks, ergonomic design, USB-C fast charging', 2, 2, 65.00, 99.99, 'unit', 25, 100, TRUE),
(5, 'PROD-PER-002', 'Keychron K2 Pro Mechanical Keyboard', 'Wireless Custom Mechanical Keyboard, QMK/VIA support, Hot-swappable', 2, 2, 70.00, 109.99, 'unit', 20, 50, TRUE),
(6, 'PROD-PER-003', 'Dell UltraSharp 27 4K USB-C Monitor (U2723QE)', 'IPS Black technology, 2000:1 contrast, 98% DCI-P3, USB-C Hub', 2, 1, 420.00, 580.00, 'unit', 15, 40, TRUE),
(7, 'PROD-NET-001', 'Cisco Catalyst 9200L 48-port Switch', 'PoE+ 48 ports, 4x 10G uplink, Network Essentials', 3, 3, 2100.00, 2850.00, 'unit', 5, 10, TRUE),
(8, 'PROD-NET-002', 'Ubiquiti UniFi WiFi 6 Enterprise Access Point', 'Tri-band WiFi 6E, 10.6 Gbps aggregate throughput, 2.5GbE', 3, 3, 230.00, 329.00, 'unit', 15, 30, TRUE),
(9, 'PROD-NET-003', 'MikroTik CCR2004 16G-2S+ Router', '16x Gigabit Ethernet, 2x 10G SFP+ ports, Quad-core ARM CPU', 3, 3, 310.00, 425.00, 'unit', 10, 20, TRUE),
(10, 'PROD-STO-001', 'Samsung 990 PRO 2TB NVMe SSD', 'PCIe Gen4 x4 M.2 2280, up to 7450 MB/s read', 4, 4, 130.00, 179.99, 'unit', 30, 80, TRUE),
(11, 'PROD-STO-002', 'Synology DiskStation DS923+ NAS', '4-bay NAS enclosure, AMD Ryzen R1600, 4GB DDR4 ECC', 4, 4, 450.00, 599.99, 'unit', 8, 15, TRUE),
(12, 'PROD-STO-003', 'WD Red Plus 12TB NAS Hard Drive', '3.5" SATA 6Gb/s 7200 RPM 256MB Cache for multi-bay NAS', 4, 4, 210.00, 289.99, 'unit', 20, 40, TRUE),
(13, 'PROD-ERG-001', 'Herman Miller Aeron Ergonomic Chair', 'Fully adjustable postureFit SL, mineral frame, leather armpads', 5, 5, 950.00, 1395.00, 'unit', 6, 12, TRUE),
(14, 'PROD-ERG-002', 'Fully Jarvis Bamboo Standing Desk (60x30)', 'Dual motor, programmable memory handset, cable management tray', 5, 5, 520.00, 749.00, 'unit', 8, 15, TRUE),
(15, 'PROD-LAP-004', 'HP EliteBook 840 G10', '14" FHD IPS, Intel Core i5 13th Gen, 16GB RAM, 512GB SSD', 1, 1, 880.00, 1150.00, 'unit', 15, 30, TRUE),
(16, 'PROD-LAP-005', 'ASUS ROG Zephyrus G14', '14" 165Hz QHD, AMD Ryzen 9 7940HS, RTX 4070, 32GB RAM', 1, 2, 1600.00, 1999.99, 'unit', 5, 10, TRUE),
(17, 'PROD-PER-004', 'Sony WH-1000XM5 Wireless Headphones', 'Industry-leading noise canceling, auto NC optimizer, 30hr battery', 2, 2, 280.00, 399.99, 'unit', 18, 40, TRUE),
(18, 'PROD-PER-005', 'Anker 737 Power Bank (PowerCore 24K)', '24000mAh 3-port portable charger with 140W output and smart display', 2, 2, 90.00, 149.99, 'unit', 25, 60, TRUE),
(19, 'PROD-NET-004', 'Fortinet FortiGate 60F Firewall', 'Next-Gen Firewall, 10x GE RJ45 ports, enterprise threat protection', 3, 3, 750.00, 1050.00, 'unit', 6, 12, TRUE),
(20, 'PROD-ERG-003', 'Ergotron LX Dual Stacking Arm', 'Polished aluminum desk mount dual monitor arm with tall pole', 5, 5, 230.00, 349.00, 'unit', 12, 25, TRUE);

-- Inventories across 3 Warehouses
INSERT INTO inventories (id, product_id, warehouse_id, available_quantity, reserved_quantity, damaged_quantity, version, updated_at) VALUES
(1, 1, 1, 18, 4, 0, 0, CURRENT_TIMESTAMP),
(2, 1, 2, 12, 0, 0, 0, CURRENT_TIMESTAMP),
(3, 2, 1, 8, 2, 0, 0, CURRENT_TIMESTAMP),
(4, 2, 3, 5, 0, 1, 0, CURRENT_TIMESTAMP),
(5, 3, 1, 6, 0, 0, 0, CURRENT_TIMESTAMP),
(6, 4, 1, 45, 5, 2, 0, CURRENT_TIMESTAMP),
(7, 4, 2, 38, 0, 0, 0, CURRENT_TIMESTAMP),
(8, 5, 1, 14, 0, 0, 0, CURRENT_TIMESTAMP),
(9, 6, 1, 9, 1, 0, 0, CURRENT_TIMESTAMP),
(10, 7, 2, 3, 0, 0, 0, CURRENT_TIMESTAMP),
(11, 8, 1, 22, 0, 0, 0, CURRENT_TIMESTAMP),
(12, 9, 2, 7, 0, 0, 0, CURRENT_TIMESTAMP),
(13, 10, 1, 55, 10, 0, 0, CURRENT_TIMESTAMP),
(14, 11, 1, 4, 0, 0, 0, CURRENT_TIMESTAMP),
(15, 12, 3, 16, 0, 0, 0, CURRENT_TIMESTAMP),
(16, 13, 1, 2, 0, 0, 0, CURRENT_TIMESTAMP),
(17, 14, 2, 5, 0, 0, 0, CURRENT_TIMESTAMP),
(18, 15, 1, 11, 0, 0, 0, CURRENT_TIMESTAMP),
(19, 16, 1, 3, 0, 0, 0, CURRENT_TIMESTAMP),
(20, 17, 2, 28, 4, 0, 0, CURRENT_TIMESTAMP),
(21, 18, 1, 40, 0, 0, 0, CURRENT_TIMESTAMP),
(22, 19, 3, 4, 0, 0, 0, CURRENT_TIMESTAMP),
(23, 20, 1, 15, 0, 0, 0, CURRENT_TIMESTAMP);

-- Initial Reservations
INSERT INTO reservations (id, product_id, warehouse_id, reference_number, quantity, status, created_by, created_at) VALUES
(1, 1, 1, 'ORD-2026-8801', 4, 'ACTIVE', 2, CURRENT_TIMESTAMP),
(2, 2, 1, 'ORD-2026-8802', 2, 'ACTIVE', 2, CURRENT_TIMESTAMP),
(3, 4, 1, 'ORD-2026-8803', 5, 'ACTIVE', 3, CURRENT_TIMESTAMP);

-- Stock Transactions Ledger
INSERT INTO stock_transactions (id, product_id, warehouse_id, transaction_type, quantity, previous_available_quantity, new_available_quantity, previous_reserved_quantity, new_reserved_quantity, previous_damaged_quantity, new_damaged_quantity, reason, reference_number, notes, performed_by, created_at) VALUES
(1, 1, 1, 'STOCK_IN', 30, 0, 30, 0, 0, 0, 0, 'Initial Stock Receipt', 'IN-001', 'Received initial inventory batch', 1, '2026-08-01 10:00:00'),
(2, 1, 1, 'STOCK_OUT', 8, 30, 22, 0, 0, 0, 0, 'Order Fulfillment', 'SO-101', 'Customer Order #101', 3, '2026-08-10 14:30:00'),
(3, 1, 1, 'RESERVE', 4, 22, 18, 0, 4, 0, 0, 'Sales Order Allocation', 'ORD-2026-8801', 'Reserved for enterprise client shipment', 2, '2026-08-22 09:15:00'),
(4, 3, 1, 'STOCK_IN', 15, 0, 15, 0, 0, 0, 0, 'Supplier Delivery', 'IN-002', 'Received shipment from Nexus HW', 2, '2026-08-05 11:00:00'),
(5, 3, 1, 'STOCK_OUT', 9, 15, 6, 0, 0, 0, 0, 'Internal IT Allocation', 'SO-102', 'Deploying to new engineering batch', 3, '2026-08-18 16:45:00'),
(6, 4, 1, 'TRANSFER_OUT', 10, 55, 45, 5, 5, 0, 0, 'Inter-Warehouse Transfer', 'TR-881', 'Transferring 10 units to HYD-01', 2, '2026-08-25 10:00:00'),
(7, 4, 2, 'TRANSFER_IN', 10, 28, 38, 0, 0, 0, 0, 'Inter-Warehouse Transfer', 'TR-881', 'Received 10 units from BLR-01', 2, '2026-08-25 10:05:00'),
(8, 4, 1, 'DAMAGE', 2, 45, 45, 5, 5, 0, 2, 'Quality Inspection Failure', 'DMG-01', 'Packaging damaged in transit', 2, '2026-08-26 15:20:00');

-- Purchase Orders
INSERT INTO purchase_orders (id, po_number, supplier_id, warehouse_id, status, order_date, expected_delivery_date, total_amount, notes, created_by, created_at) VALUES
(1, 'PO-2026-001', 1, 1, 'ORDERED', '2026-08-20', '2026-08-30', 28500.00, 'Urgent stock replenishment for Dell Laptops', 2, CURRENT_TIMESTAMP),
(2, 'PO-2026-002', 2, 1, 'APPROVED', '2026-08-24', '2026-09-02', 7000.00, 'Reorder mechanical keyboards and peripherals', 2, CURRENT_TIMESTAMP),
(3, 'PO-2026-003', 3, 2, 'PENDING', '2026-08-27', '2026-09-08', 10500.00, 'Networking switches for Hyderabad hub expansion', 2, CURRENT_TIMESTAMP);

INSERT INTO purchase_order_items (id, purchase_order_id, product_id, ordered_quantity, received_quantity, unit_cost, total_cost) VALUES
(1, 1, 1, 30, 0, 950.00, 28500.00),
(2, 2, 5, 100, 0, 70.00, 7000.00),
(3, 3, 7, 5, 0, 2100.00, 10500.00);

-- Audit Logs
INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, description, created_at) VALUES
(1, 1, 'SYSTEM_INITIALIZATION', 'SYSTEM', 1, 'Database schema V1 and seed data V2 migrated successfully.', CURRENT_TIMESTAMP),
(2, 2, 'PURCHASE_ORDER_CREATED', 'PURCHASE_ORDER', 1, 'Created PO-2026-001 for TechPulse Global Inc.', CURRENT_TIMESTAMP);
