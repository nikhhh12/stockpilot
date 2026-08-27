# StockPilot AI — Intelligent Inventory Management & Optimization Platform

StockPilot AI is an enterprise-grade full-stack inventory management and optimization platform built with Java 17, Spring Boot 3.x, MySQL, Flyway migrations, Spring Security with JWT, React 18, TypeScript, Vite, Tailwind CSS, deterministic statistical analytics, and controlled, read-only generative AI.

## Product Architecture

```text
StockPilot AI
│
├── Core Inventory Management (Products, Categories, Suppliers, Warehouses, Inventory, Reserve/Release, Transfers)
├── Procurement Workflows (Purchase Orders, Dynamic Items, Receiving, Auto Inventory Sync)
├── Audit Ledger (Stock Transactions & System Audit Logs)
├── Statistical Intelligence (Moving Average & Exponential Smoothing Demand Forecast, Stockout Risk, Reorder Recs, Z-Score Anomalies)
└── Read-Only Generative AI (AI Assistant Chat, Executive Daily Brief, RBAC Data Scoped Tools)
```

## Getting Started

### Environment Variables
Copy `.env.example` to `.env` and fill in your MySQL and optional AI provider credentials:
```bash
cp .env.example .env
```

### Local Execution (Maven & Node)
```bash
# Backend
cd backend
export PATH="/opt/homebrew/bin:$PATH"
mvn spring-boot:run

# Frontend
cd frontend
npm install
npm run dev
```

### Docker Execution
```bash
docker-compose up --build
```
