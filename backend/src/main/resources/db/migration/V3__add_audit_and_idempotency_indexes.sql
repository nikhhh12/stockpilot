-- Flyway Migration V3: Add query performance indexes for audit log and transaction idempotency

CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type);
CREATE INDEX idx_audit_created ON audit_logs(created_at);
CREATE INDEX idx_st_ref_type ON stock_transactions(reference_number, transaction_type);
