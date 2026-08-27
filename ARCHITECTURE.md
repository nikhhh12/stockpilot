# StockPilot AI — System Architecture & Design Specification (Frozen)

## 1. Domain Quantity Semantics & Invariants

$$\text{onHandQuantity} = \text{availableQuantity} + \text{reservedQuantity} + \text{damagedQuantity}$$

- **Authoritative Backend**: All quantity mutations are verified and executed inside Spring Boot `@Transactional` boundaries. Frontend never overrides server inventory state.
- **Optimistic Locking**: `@Version` field on `Inventory` entity prevents race conditions. Concurrent conflicts return `HTTP 409 CONFLICT` (`INVENTORY_CONFLICT`) with message: `"Inventory was updated by another user. Refresh and retry."`
- **Mutation Operations & Rules**:
  - `STOCK_IN`: `available += N`
  - `STOCK_OUT`: `available -= N`
  - `RESERVE`: `available -= N`, `reserved += N`
  - `RELEASE`: `reserved -= N`, `available += N`
  - `DAMAGE`: `available -= N`, `damaged += N`
  - `RETURN`: `available += N`
  - `TRANSFER_OUT`: `source.available -= N`
  - `TRANSFER_IN`: `destination.available -= N`
  - All quantities must remain non-negative ($\ge 0$).

---

## 2. Statistical Intelligence (Deterministic & Independent of LLM)

Operates 100% reliably without requiring any external AI provider or LLM configuration:

### 2.1 Demand Forecasting Architecture
```text
DemandForecastService
        |
        +-- MovingAverageForecastAlgorithm (7-day / 14-day / 30-day windowed historical demand average)
        |
        +-- ExponentialSmoothingForecastAlgorithm (Alpha-weighted smoothing prioritizing recent demand velocity)
```
- When historical data points $< 3$ observations / distinct periods, returns `INSUFFICIENT_DATA` safely without guessing.

### 2.2 Stockout Risk Boundaries
- `daysRemaining > 30` &rarr; `LOW`
- `daysRemaining > 14 && <= 30` &rarr; `MEDIUM`
- `daysRemaining > 6 && <= 14` &rarr; `HIGH`
- `daysRemaining <= 6` &rarr; `CRITICAL`
- Configurable threshold parameters in application settings.

### 2.3 Reorder Recommendation
$$\text{Recommended Reorder Quantity} = \max(0, (\text{Average Daily Demand} \times \text{Lead Time Days}) + \text{Safety Stock} - (\text{Available} + \text{OnOrder}))$$

### 2.4 Z-Score Anomaly Detection
$$\text{absoluteZScore} = \frac{|X - \mu|}{\sigma}$$
- If standard deviation $\sigma = 0$, anomaly scoring returns status `NO_VARIANCE` rather than generating false anomaly flags.
- When $\sigma > 0$, transactions with $\text{absoluteZScore} \ge 2.5$ are flagged with anomaly severity.

---

## 3. Generative AI Engine (Read-Only & Authorization-Aware)

- Strictly read-only: No mutations, no SQL execution, zero write permissions.
- Accesses domain state solely through controlled, user-context-aware tools:
  - `getInventorySummary()`
  - `getLowStockProducts()`
  - `getOutOfStockProducts()`
  - `getStockoutRisks()`
  - `getReorderRecommendations()`
  - `getRecentTransactions()`
  - `getInventoryByCategory()`
  - `getInventoryByWarehouse()`
  - `getSupplierStatistics()`
  - `getPurchaseOrderSummary()`
  - `getAnomalies()`
- **Graceful Failure Isolation**: If the LLM provider fails, times out, or has no API key configured, core inventory, CRUD, transactions, purchase orders, forecasting, and anomaly detection continue running uninterrupted.

---

## 4. Purchase Order Lifecycle & Receiving

```text
DRAFT -> PENDING -> APPROVED -> ORDERED -> PARTIALLY_RECEIVED -> RECEIVED
```
- Order placement: `POST /api/purchase-orders/{id}/order` (requires `APPROVED` status).
- Atomic Receiving: `0 < delta <= (ordered - received)`. Increments `availableQuantity`, creates `STOCK_IN` transaction, updates PO status to `PARTIALLY_RECEIVED` or `RECEIVED`. No over-receiving.
