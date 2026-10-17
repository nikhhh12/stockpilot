# StockPilot AI — Intelligent Enterprise Inventory Management & Optimization Platform

StockPilot AI is a production-grade, full-stack inventory management, statistical optimization, and decision-support platform. Built with **Java 17**, **Spring Boot 3.2.5**, **MySQL 8**, **Flyway**, **Spring Security (JJWT)**, **React 18**, **TypeScript**, **Vite**, **Tailwind CSS**, and **Docker Compose**, it enforces rigorous transactional invariants, optimistic locking concurrency control, server-side Role-Based Access Control (RBAC), deterministic demand forecasting, and a grounded, read-only AI assistant.

---

## 1. System Architecture & High-Level Flow

```text
                                +-----------------------------------+
                                |   Client Browser (React 18 SPA)   |
                                +-----------------+-----------------+
                                                  |
                                       HTTP / REST (JWT Bearer)
                                                  |
                                                  v
                                +-----------------------------------+
                                |   Nginx Reverse Proxy (:80/:3000) |
                                +--------+-----------------+--------+
                                         |                 |
                         / (SPA Static Assets)       /api/ (API Proxy)
                                         |                 |
                                         v                 v
                       +--------------------+   +------------------------------------+
                       | Nginx HTML5 Bundle |   | Spring Boot 3.2.5 Application      |
                       +--------------------+   |  - CorrelationIdFilter (MDC)       |
                                                |  - SecurityConfig (Stateless JWT)  |
                                                |  - Method Security (@PreAuthorize) |
                                                |  - GlobalExceptionHandler (409/403)|
                                                +--------+-----------------+---------+
                                                         |                 |
                                          JPA / Hibernate (MySQL 8)   HTTP REST (Read-Only)
                                                         |                 |
                                                         v                 v
                                                +----------------+  +----------------+
                                                | MySQL 8 Schema |  | LLM Provider   |
                                                | - V1 Schema    |  | (OpenAI/Gemini |
                                                | - V2 Seed Data |  |  or Deterministic
                                                | - Unique Idemp |  |  Fallback)     |
                                                +----------------+  +----------------+
```

---

## 2. Core Domain Invariants & Concurrency

### 2.1 Inventory Invariant Equation
The system strictly enforces the following quantity invariant at all times:
$$\text{onHandQuantity} = \text{availableQuantity} + \text{reservedQuantity} + \text{damagedQuantity}$$

- `onHandQuantity` is **derived** (`@Transient`) from the physical components, eliminating dual sources of truth.
- All quantities are non-negative ($\ge 0$).
- Deductions from `availableQuantity` that exceed on-hand available stock are rejected immediately with `INSUFFICIENT_STOCK`.

### 2.2 Concurrency & Optimistic Locking
- The `Inventory` entity utilizes a JPA `@Version` column.
- Concurrent updates to the same inventory record trigger an `ObjectOptimisticLockingFailureException`.
- The `GlobalExceptionHandler` translates this into an **HTTP 409 CONFLICT** response (`INVENTORY_CONFLICT`) with a clear user prompt: *"Inventory was updated by another user. Please refresh and retry."*
- The frontend TanStack React Query invalidates cached state and informs the user rather than blindly replaying the mutation.

### 2.3 Persistent Idempotency
- **Warehouse Transfers**: Requests accept a unique `referenceNumber`. If a transfer with that reference has already been executed (`TRANSFER_OUT`), the backend returns the current inventory state idempotently without executing duplicate movements.
- **Purchase Order Receipts**: Requests accept a unique `receiptReference`. Repeated submissions with the same receipt identifier are recognized and return the current purchase order state without duplicate stock-in increments.

---

## 3. Role-Based Access Control (RBAC) Matrix

Server-side authorization is enforced via Spring Security `@PreAuthorize` method annotations and custom JSON `AuthenticationEntryPoint` (HTTP 401) and `AccessDeniedHandler` (HTTP 403).

| Resource / Action | ADMIN | INVENTORY_MANAGER | STAFF |
|---|:---:|:---:|:---:|
| **Authentication & Profile** | Full | Full | Full |
| **User & Role Administration** (`/api/users/**`) | Full Access | No Access (403) | No Access (403) |
| **System Settings Mutation** (`/api/settings/config`) | Full Access | Full Access | Read-Only (403 on write) |
| **System Audit Logs** (`/api/audit-logs/**`) | Read Access | Read Access | No Access (403) |
| **Physical Stock Ledger** (`/api/transactions/**`) | Read Access | Read Access | Read Access |
| **Category Creation / Deletion** | Full Access | Create/Update Only | Read-Only |
| **Product / Supplier / Warehouse Management** | Full Access | Full Access | Read-Only |
| **Inventory Routine Operations** (Stock In, Out, Reserve, Release) | Full Access | Full Access | Full Access |
| **Inventory Discretionary Adjustments** (`/adjust`) | Full Access | Full Access | No Access (403) |
| **Purchase Order Creation / Draft** | Full Access | Full Access | Read-Only |
| **Purchase Order Approval** (`/approve`) | Full Access | Full Access | No Access (403) |
| **Purchase Order Ordering** (`/order`) | Full Access | Full Access | No Access (403) |
| **Purchase Order Receiving** (`/receive`) | Full Access | Full Access | Permitted |
| **Demand Forecasting & Risk Analytics** | Full Access | Full Access | Full Access |
| **AI Command Center & Assistant** (Read-Only) | Full Access | Full Access | Full Access |

---

## 4. Purchase Order Lifecycle

```text
[ DRAFT ] ──> [ PENDING ] ──> [ APPROVED ] ──> [ ORDERED ] ──> [ PARTIALLY_RECEIVED ] ──> [ RECEIVED ]
    │              │                                                    ▲                     │
    └── CANCELLED ─┴────────────────────────────────────────────────────┴─────────────────────┘
```

1. **Approval Gate**: Orders cannot be placed (`ORDERED`) without explicit prior approval (`APPROVED`) by an Administrator or Inventory Manager.
2. **Atomic Receiving**: Line-item quantities are verified such that $\text{receivedQuantity} \le \text{orderedQuantity}$. Over-receiving is prevented.
3. **Ledger Integration**: Each receipt automatically creates an atomic `STOCK_IN` transaction and updates available inventory within the same database transaction.

---

## 5. Statistical Intelligence & Analytics

1. **Demand Forecasting**:
   - **Moving Average**: Windowed average across customizable horizons (7, 14, 30 days). Requires $\ge 3$ data points; otherwise returns `INSUFFICIENT_DATA`.
   - **Exponential Smoothing**: Alpha-weighted ($0 < \alpha \le 1$) forecasting emphasizing recent demand velocity. Handles zero-variance and demand spikes safely.
2. **Stockout Risk**:
   - Classifies risk levels (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`) based on estimated days remaining:
     $$\text{Days Remaining} = \frac{\text{availableQuantity}}{\text{averageDailyDemand}}$$
3. **Reorder Recommendations**:
   $$\text{Reorder Quantity} = \max(0, (\text{Average Daily Demand} \times \text{Lead Time}) + \text{Safety Stock} - (\text{Available} + \text{Pipeline On-Order}))$$
4. **Z-Score Anomaly Detection**:
   $$Z = \frac{|X - \mu|}{\sigma}$$
   Flags transactions with $Z \ge 2.5$. If $\sigma = 0$, returns `NO_VARIANCE` to eliminate false alarms.

---

## 6. Live AI Assistant & Grounded Decision Support

- **Zero Heavy Dependencies**: Uses Spring 3.2 built-in `RestClient` with strict 5-second connect and 10-second read timeouts.
- **Strict Read-Only Boundary**: The AI assistant cannot invoke mutations, execute arbitrary SQL, or alter permissions. It only consumes factual, authorized read-only tool responses:
  - `getInventorySummary`, `getStockoutRisks`, `getReorderRecommendations`, `getRecentTransactions`, `getAnomalies`, `getPurchaseOrderSummary`.
- **Deterministic Fallback**: If `AI_ENABLED=false` or external provider calls fail or rate-limit, the system falls back seamlessly to deterministic analytics templates without impacting core inventory workflows.

---

## 7. Observability & Security Hardening

- **End-to-End Tracing**: `CorrelationIdFilter` captures or generates `X-Correlation-ID`, propagating it to response headers and SLF4J `MDC` for structured console logging.
- **Sanitized Errors**: Exception handlers return clean, RFC-compliant JSON errors without exposing SQL queries or stack traces.
- **Actuator Security**: `/actuator/health` and `/actuator/info` are public for container health checks; `/actuator/**` metrics and administration are restricted to `ADMIN`.
- **Dual Audit System**:
  - **StockTransaction Ledger**: Immutable physical stock movement records (deltas, balances, reference numbers, actors).
  - **AuditLog Trail**: Governance records tracking authentication, role modifications, entity lifecycle events, and security access attempts.

---

## 8. Getting Started & Verification Commands

### 8.1 Prerequisites
- **Java 17 LTS** (Temurin/OpenJDK)
- **Node.js 20+** and npm
- **Docker & Docker Compose** (for containerized deployment)
- **MySQL 8** (for production profile)

### 8.2 Local Development Setup

1. **Environment Configuration**:
   ```bash
   cp .env.example .env
   ```

2. **Backend (Dev Profile with H2 In-Memory Database)**:
   ```bash
   cd backend
   export PATH="/opt/homebrew/bin:$PATH" # Mac Homebrew path if applicable
   mvn clean test                      # Run all 42 automated tests
   mvn spring-boot:run                 # Starts server on http://localhost:8080
   ```

3. **Frontend (Vite Dev Server)**:
   ```bash
   cd frontend
   npm ci
   npm run build                       # Verify TypeScript compilation and bundle
   npm run dev                         # Starts dev server on http://localhost:3000
   ```

4. **Access Endpoints**:
   - Web Application: [http://localhost:3000](http://localhost:3000)
   - Swagger / OpenAPI UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   - Actuator Health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

### 8.3 Default Seed Credentials (Development / Demo Profile)
- **Admin**: `admin@stockpilot.io` / `adminpassword`
- **Manager**: `manager@stockpilot.io` / `managerpassword`
- **Staff**: `staff@stockpilot.io` / `staffpassword`

---

## 9. Docker Compose Full-Stack Deployment

The project includes a production-ready, multi-stage Docker Compose setup:
- **MySQL 8.0**: Persistent volume, UTF8MB4, health check.
- **Spring Boot Backend**: Production profile, connects via internal service networking, health check on `/actuator/health`.
- **Frontend Nginx**: Multi-stage build (`node:20-alpine` -> `nginx:alpine`), SPA fallback routing, and reverse proxy for `/api/`.

```bash
# Build and start all three containers in detached mode
docker-compose up --build -d

# Check health and status of containers
docker-compose ps

# View application logs with request correlation IDs
docker-compose logs -f backend

# Stop the application stack
docker-compose down
```

---

## 10. Automated Testing & Quality Results

| Test Category | Suite File | Tests Run | Result |
|---|---|:---:|:---:|
| **Security & RBAC** | `SecurityRbacIntegrationTest` | 9 | **PASS** |
| **Inventory Invariants & Mutations** | `InventoryInvariantAndMutationTest` | 8 | **PASS** |
| **Purchase Order Lifecycle & Idempotency** | `PurchaseOrderLifecycleTest` | 3 | **PASS** |
| **Statistical Intelligence** | `StatisticalIntelligenceTest` | 6 | **PASS** |
| **Optimistic Locking** | `InventoryOptimisticLockingTest` | 1 | **PASS** |
| **Controller Integration** | `ControllerIntegrationTests` | 11 | **PASS** |
| **AI Grounding & Tool Safety** | `AIServiceTest` | 3 | **PASS** |
| **Frontend TypeScript Check** | `tsc && vite build` | 2,405 modules | **PASS (0 errors)** |
| **Total Automated Tests** | — | **42 / 42** | **100% PASS** |

---

## 11. Known Limitations & Future Work

1. **LLM Integration**: Live conversational features require a valid OpenAI or Gemini API key set in `AI_API_KEY`. When unconfigured, the application runs reliably in deterministic analytics fallback mode.
2. **Distributed Cache**: The current architecture uses MySQL database transactions and Hibernate optimistic locking. High-throughput multi-region replication would benefit from an external Redis cluster for distributed rate limiting.
3. **Database Backup Automation**: Production deployments should establish automated mysqldump or cloud-native snapshots on the `mysql_data` volume.
