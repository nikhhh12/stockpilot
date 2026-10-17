# StockPilot AI — Deep Architectural & Engineering Specification

## 1. Domain Model, Quantity Semantics & Invariants

### 1.1 Invariant Formulation
StockPilot AI models multi-warehouse inventory with mathematical precision. For any product $p$ in warehouse $w$:

$$\text{onHandQuantity} = \text{availableQuantity} + \text{reservedQuantity} + \text{damagedQuantity}$$

- **Single Source of Truth**: `onHandQuantity` is marked as `@Transient` in `com.stockpilot.entity.Inventory` and calculated dynamically by summing `availableQuantity`, `reservedQuantity`, and `damagedQuantity`. This avoids synchronization anomalies and dual sources of truth.
- **Non-Negativity Constraint**: All quantities are verified by service validations and database constraints ($\ge 0$). Any operation attempting to deduct quantities beyond available inventory throws `StockPilotException("INSUFFICIENT_STOCK", HttpStatus.BAD_REQUEST)`.

### 1.2 State Transition Matrix for Inventory Movements

| Operation | Affected Fields | Transaction History Type | Reversibility |
|---|---|---|---|
| **Stock In** | `available += N` | `STOCK_IN` | Via Stock Out or Adjustment |
| **Stock Out** | `available -= N` | `STOCK_OUT` | Via Return or Stock In |
| **Reserve** | `available -= N`, `reserved += N` | `RESERVE` | Via Release |
| **Release** | `reserved -= N`, `available += N` | `RELEASE` | Via Reserve |
| **Damage** | `available -= N`, `damaged += N` | `DAMAGE` | Via Adjustment |
| **Return** | `available += N` | `RETURN` | Via Stock Out |
| **Warehouse Transfer** | `source.available -= N`, `dest.available += N` | `TRANSFER_OUT` & `TRANSFER_IN` | Reverse Transfer |
| **Discretionary Adjust**| Explicit new values | `ADJUSTMENT` | Subsequent Adjustment |

All mutations execute inside `@Transactional(rollbackFor = Exception.class)` boundaries.

---

## 2. Concurrency, Optimistic Locking & Idempotency

### 2.1 Optimistic Locking Mechanics
- The `Inventory` entity is annotated with `@Version private Long version;`.
- When concurrent requests attempt to mutate the same inventory record, Hibernate detects the version discrepancy and throws `ObjectOptimisticLockingFailureException`.
- The `GlobalExceptionHandler` intercepts this exception and responds with:
  ```json
  {
    "message": "Inventory was updated by another user. Please refresh and retry.",
    "errorCode": "INVENTORY_CONFLICT"
  }
  ```
  HTTP Status: `409 CONFLICT`.
- Blind automated retries on the backend are avoided to prevent inconsistent state accumulation. Instead, the frontend invalidates React Query cache and notifies the user to retry with fresh data.

### 2.2 Persistent Idempotency Engine
To prevent duplicate execution of high-impact mutations caused by network timeouts or repeated button clicks:
1. **Warehouse Transfers**:
   - Accepts a `referenceNumber` in `TransferRequest`.
   - `StockTransactionRepository.existsByReferenceNumberAndTransactionType(referenceNumber, TRANSFER_OUT)` checks for a previously recorded transfer under that identifier.
   - If present, the service logs a warning and returns the current inventory state idempotently without mutating balances again.
2. **Purchase Order Receiving**:
   - Accepts a `receiptReference` in `PurchaseOrderReceiveRequest`.
   - If a line-item transaction with that receipt reference already exists, the service returns the existing PO state without incrementing received counts or inventory.

---

## 3. Purchase Order State Machine & Procurement

```text
       +-------+
       | DRAFT |
       +---+---+
           | (Submit)
           v
       +-------+
       |PENDING| <------------------------------------+
       +---+---+                                      |
           | (Approve - ADMIN/INVENTORY_MANAGER)      |
           v                                          |
      +--------+                                      |
      |APPROVED|                                      |
      +----+---+                                      |
           | (Place Order - ADMIN/INVENTORY_MANAGER)  |
           v                                          |
       +-------+                                      |
       |ORDERED|                                      |
       +---+---+                                      |
           | (Receive partial quantity)               |
           v                                          |
+---------------------+ (Cancel)                      |
| PARTIALLY_RECEIVED  | ------------------------------+
+----------+----------+                               |
           | (Receive all remaining units)            |
           v                                          |
     +----------+                                     |
     | RECEIVED |                                     |
     +----------+                                     |
           ^                                          |
           +------------------------------------------+
```

### 3.1 Strict State Transition Rules
- **No Direct Ordering**: Orders cannot transition to `ORDERED` without being in `APPROVED` state first.
- **Role Verification**: Only users with `ROLE_ADMIN` or `ROLE_INVENTORY_MANAGER` can approve or order purchase orders. Staff cannot approve.
- **Receiving Invariant**: For each line item:
  $$0 < \Delta \text{received} \le (\text{orderedQuantity} - \text{alreadyReceivedQuantity})$$
  Over-receiving is blocked. When all line items have $\text{receivedQuantity} == \text{orderedQuantity}$, the PO status transitions automatically to `RECEIVED`.

---

## 4. Server-Side Security & RBAC Architecture

### 4.1 Filter Chain Sequence
1. **CorrelationIdFilter**: Generates/propagates `X-Correlation-ID` header and populates SLF4J MDC.
2. **JwtAuthenticationFilter**:
   - Extracts Bearer token from `Authorization` header.
   - Validates HMAC-SHA256 signature and expiration via `JwtTokenProvider`.
   - Loads `UserDetails` and establishes `SecurityContextHolder`.
3. **AuthorizationFilter**:
   - Evaluates Spring Security method-level `@PreAuthorize("hasRole('...')")`.
   - Rejects unauthenticated requests with JSON `AuthenticationEntryPoint` (`401 UNAUTHORIZED`).
   - Rejects unauthorized requests with JSON `AccessDeniedHandler` (`403 FORBIDDEN`).

### 4.2 Actor Attribution
All inventory operations and audit log entries retrieve the active caller via `SecurityUtils.getCurrentUserEmail()`. The database user is dynamically resolved, ensuring immutable attribution to the actual authenticated actor rather than falling back to static IDs.

---

## 5. Statistical Forecasting & Analytics

### 5.1 Demand Forecasting
- **Moving Average ($MA_k$)**:
  $$\hat{D}_{t+1} = \frac{1}{k}\sum_{i=0}^{k-1} D_{t-i}$$
  Tested across 7, 14, and 30-day windows. Requires $\ge 3$ data points; otherwise returns `INSUFFICIENT_DATA`.
- **Exponential Smoothing ($ES_\alpha$)**:
  $$\hat{D}_{t+1} = \alpha D_t + (1 - \alpha) \hat{D}_t, \quad 0 < \alpha \le 1$$
  Defaults to $\alpha = 0.3$. Handles demand spikes, constant demand, and zero-variance datasets without negative forecasts.

### 5.2 Stockout Risk Scoring
$$\text{Days Remaining} = \begin{cases} \frac{\text{availableQuantity}}{\text{averageDailyDemand}} & \text{if } \text{averageDailyDemand} > 0 \\ \infty & \text{if } \text{averageDailyDemand} = 0 \end{cases}$$
- `Days <= 6`: `CRITICAL`
- `6 < Days <= 14`: `HIGH`
- `14 < Days <= 30`: `MEDIUM`
- `Days > 30`: `LOW`

### 5.3 Reorder Recommendation Formula
$$\text{Reorder Quantity} = \max\left(0, (\text{Average Daily Demand} \times L) + SS - (I_{\text{avail}} + PO_{\text{on\_order}})\right)$$
Where $L$ is supplier lead time in days, $SS$ is safety stock ($\text{safety\_factor} \times \sigma_{\text{demand}}$), $I_{\text{avail}}$ is available inventory, and $PO_{\text{on\_order}}$ is pipeline inbound purchase order quantity. Damaged stock is excluded from available position.

### 5.4 Consumption Anomaly Detection
$$Z = \frac{|X - \mu|}{\sigma}$$
- Sample size requirement: $\ge 3$ transactions.
- If $\sigma = 0$, anomaly detector classifies the transaction as `NO_VARIANCE` to avoid false positives.
- If $Z \ge 2.5$, flagged as `STATISTICAL_ANOMALY`.

---

## 6. Generative AI Engine & Safety Boundaries

```text
[ User Prompt ] ──> [ AIServiceImpl ]
                           │
                 +---------+---------+
                 | Parse Intent &    |
                 | Fetch Safe Tools  |
                 +---------+---------+
                           │
       +-------------------+-------------------+
       │ (Grounded Facts via Read-Only Tools)  │
       v                                       v
[ LlmClient (RestClient) ]            [ Deterministic Fallback ]
  - 5s Connect Timeout                  - Used if AI_ENABLED=false
  - 10s Read Timeout                    - Used if API Key missing
  - OpenAI / Gemini format              - Used on Rate Limit / Timeout
       │                                       │
       +-------------------+-------------------+
                           │
                           v
               [ Grounded Decision Brief ]
```

### 6.1 Tool Sandbox Rules
1. Tools are purely **read-only**: `getInventorySummary`, `getStockoutRisks`, `getReorderRecommendations`, `getRecentTransactions`, `getAnomalies`.
2. Model cannot generate arbitrary SQL or execute repository mutation methods.
3. If external LLM calls fail or timeout, deterministic summaries are produced without interrupting inventory management operations.

---

## 7. Dual Ledger Architecture

1. **Physical Stock Transaction Ledger (`stock_transactions`)**:
   - Append-only physical inventory movements.
   - Records previous available, reserved, and damaged balances along with new balances and quantity delta.
   - Stores reference numbers and actor ID.
2. **System Audit Log Trail (`audit_logs`)**:
   - Governance and compliance record.
   - Captures user authentication events, role changes, product and supplier creations, deletions, and administrative actions.
   - Exposed to `ADMIN` and `INVENTORY_MANAGER` through `/api/audit-logs` and the frontend Audit Logs view.
