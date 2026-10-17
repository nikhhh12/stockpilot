import axios from 'axios';
import {
  ApiResponse,
  PagedResponse,
  AuthResponse,
  User,
  Category,
  Supplier,
  Warehouse,
  Product,
  Inventory,
  StockTransaction,
  PurchaseOrder,
  DemandForecast,
  StockoutRisk,
  ReorderRecommendation,
  AnomalyDetection,
  AIDailyBrief,
  AIChatResponse,
  DashboardSummary,
  SystemConfigResponse,
  AuditLog,
} from '../types';

const api = axios.create({
  baseURL: (import.meta as any).env?.VITE_API_BASE_URL || '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach JWT token automatically
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('stockpilot_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor with 409 conflict handling notification
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 409) {
      console.warn('Inventory Concurrency Conflict Detected (409): Refreshing latest server state...');
    }
    if (error.response?.status === 401) {
      if (!window.location.pathname.includes('/login') && !window.location.pathname.includes('/register')) {
        localStorage.removeItem('stockpilot_token');
        localStorage.removeItem('stockpilot_user');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// Auth Services
export const authApi = {
  login: (data: { email: string; password: string }) =>
    api.post<ApiResponse<AuthResponse>>('/auth/login', data).then((res) => res.data.data),
  register: (data: { name: string; email: string; password: string; role: string }) =>
    api.post<ApiResponse<AuthResponse>>('/auth/register', data).then((res) => res.data.data),
  getMe: () => api.get<ApiResponse<User>>('/auth/me').then((res) => res.data.data),
};

// Dashboard Services
export const dashboardApi = {
  getSummary: () => api.get<ApiResponse<DashboardSummary>>('/dashboard/summary').then((res) => res.data.data),
  getStockMovement: () => api.get<ApiResponse<any>>('/dashboard/stock-movement').then((res) => res.data.data),
  getByCategory: () => api.get<ApiResponse<any>>('/dashboard/inventory-by-category').then((res) => res.data.data),
  getByWarehouse: () => api.get<ApiResponse<any>>('/dashboard/inventory-by-warehouse').then((res) => res.data.data),
  getTopMoving: () => api.get<ApiResponse<any>>('/dashboard/top-moving-products').then((res) => res.data.data),
};

// Products Services
export const productApi = {
  getPaged: (params?: { search?: string; categoryId?: number; supplierId?: number; active?: boolean; page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<Product>>>('/products', { params }).then((res) => res.data.data),
  getAll: () => api.get<ApiResponse<Product[]>>('/products/all').then((res) => res.data.data),
  getById: (id: number) => api.get<ApiResponse<Product>>(`/products/${id}`).then((res) => res.data.data),
  getBySku: (sku: string) => api.get<ApiResponse<Product>>(`/products/sku/${sku}`).then((res) => res.data.data),
  create: (data: any) => api.post<ApiResponse<Product>>('/products', data).then((res) => res.data.data),
  update: (id: number, data: any) => api.put<ApiResponse<Product>>(`/products/${id}`, data).then((res) => res.data.data),
  delete: (id: number) => api.delete<ApiResponse<void>>(`/products/${id}`).then((res) => res.data),
};

// Inventory Services
export const inventoryApi = {
  getPaged: (params?: { search?: string; warehouseId?: number; categoryId?: number; stockStatus?: string; page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<Inventory>>>('/inventory', { params }).then((res) => res.data.data),
  getById: (id: number) => api.get<ApiResponse<Inventory>>(`/inventory/${id}`).then((res) => res.data.data),
  getByProduct: (productId: number) => api.get<ApiResponse<Inventory[]>>(`/inventory/product/${productId}`).then((res) => res.data.data),
  getByWarehouse: (warehouseId: number) => api.get<ApiResponse<Inventory[]>>(`/inventory/warehouse/${warehouseId}`).then((res) => res.data.data),
  getLowStock: () => api.get<ApiResponse<Inventory[]>>('/inventory/low-stock').then((res) => res.data.data),
  getOutOfStock: () => api.get<ApiResponse<Inventory[]>>('/inventory/out-of-stock').then((res) => res.data.data),
  stockIn: (data: { productId: number; warehouseId: number; quantity: number; reason?: string; referenceNumber?: string; notes?: string }) =>
    api.post<ApiResponse<Inventory>>('/inventory/stock-in', data).then((res) => res.data.data),
  stockOut: (data: { productId: number; warehouseId: number; quantity: number; reason?: string; referenceNumber?: string; notes?: string }) =>
    api.post<ApiResponse<Inventory>>('/inventory/stock-out', data).then((res) => res.data.data),
  adjust: (data: { productId: number; warehouseId: number; resultingQuantity: number; reason?: string; referenceNumber?: string; notes?: string }) =>
    api.post<ApiResponse<Inventory>>('/inventory/adjust', data).then((res) => res.data.data),
  reserve: (data: { productId: number; warehouseId: number; quantity: number; referenceNumber: string; notes?: string }) =>
    api.post<ApiResponse<Inventory>>('/inventory/reserve', data).then((res) => res.data.data),
  release: (data: { reservationId: number; quantity?: number; notes?: string }) =>
    api.post<ApiResponse<Inventory>>('/inventory/release', data).then((res) => res.data.data),
  damage: (data: { productId: number; warehouseId: number; quantity: number; reason?: string; referenceNumber?: string; notes?: string }) =>
    api.post<ApiResponse<Inventory>>('/inventory/damage', data).then((res) => res.data.data),
  returnStock: (data: { productId: number; warehouseId: number; quantity: number; reason?: string; referenceNumber?: string; notes?: string }) =>
    api.post<ApiResponse<Inventory>>('/inventory/return', data).then((res) => res.data.data),
  transfer: (data: { productId: number; sourceWarehouseId: number; destinationWarehouseId: number; quantity: number; referenceNumber?: string; notes?: string }) =>
    api.post<ApiResponse<Inventory[]>>('/inventory/transfer', data).then((res) => res.data.data),
};

// Transactions Services
export const transactionApi = {
  getPaged: (params?: { productId?: number; warehouseId?: number; type?: string; startDate?: string; endDate?: string; page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<StockTransaction>>>('/transactions', { params }).then((res) => res.data.data),
  getById: (id: number) => api.get<ApiResponse<StockTransaction>>(`/transactions/${id}`).then((res) => res.data.data),
  getByProduct: (productId: number) => api.get<ApiResponse<StockTransaction[]>>(`/transactions/product/${productId}`).then((res) => res.data.data),
  getByWarehouse: (warehouseId: number) => api.get<ApiResponse<StockTransaction[]>>(`/transactions/warehouse/${warehouseId}`).then((res) => res.data.data),
};

// Purchase Order Services
export const purchaseOrderApi = {
  getPaged: (params?: { search?: string; supplierId?: number; warehouseId?: number; status?: string; page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<PurchaseOrder>>>('/purchase-orders', { params }).then((res) => res.data.data),
  getAll: () => api.get<ApiResponse<PurchaseOrder[]>>('/purchase-orders/all').then((res) => res.data.data),
  getById: (id: number) => api.get<ApiResponse<PurchaseOrder>>(`/purchase-orders/${id}`).then((res) => res.data.data),
  create: (data: any) => api.post<ApiResponse<PurchaseOrder>>('/purchase-orders', data).then((res) => res.data.data),
  approve: (id: number) => api.post<ApiResponse<PurchaseOrder>>(`/purchase-orders/${id}/approve`).then((res) => res.data.data),
  order: (id: number) => api.post<ApiResponse<PurchaseOrder>>(`/purchase-orders/${id}/order`).then((res) => res.data.data),
  receive: (id: number, data: { items: Array<{ itemId: number; receivedQuantityDelta: number }> }) =>
    api.post<ApiResponse<PurchaseOrder>>(`/purchase-orders/${id}/receive`, data).then((res) => res.data.data),
  cancel: (id: number) => api.post<ApiResponse<PurchaseOrder>>(`/purchase-orders/${id}/cancel`).then((res) => res.data.data),
};

// Supplier, Warehouse & Category Services
export const supplierApi = {
  getAll: () => api.get<ApiResponse<Supplier[]>>('/suppliers/all').then((res) => res.data.data),
  getPaged: (params?: any) => api.get<ApiResponse<PagedResponse<Supplier>>>('/suppliers', { params }).then((res) => res.data.data),
  getById: (id: number) => api.get<ApiResponse<Supplier>>(`/suppliers/${id}`).then((res) => res.data.data),
  create: (data: any) => api.post<ApiResponse<Supplier>>('/suppliers', data).then((res) => res.data.data),
  update: (id: number, data: any) => api.put<ApiResponse<Supplier>>(`/suppliers/${id}`, data).then((res) => res.data.data),
  delete: (id: number) => api.delete<ApiResponse<void>>(`/suppliers/${id}`).then((res) => res.data),
};

export const warehouseApi = {
  getAll: () => api.get<ApiResponse<Warehouse[]>>('/warehouses/all').then((res) => res.data.data),
  getPaged: (params?: any) => api.get<ApiResponse<PagedResponse<Warehouse>>>('/warehouses', { params }).then((res) => res.data.data),
  getById: (id: number) => api.get<ApiResponse<Warehouse>>(`/warehouses/${id}`).then((res) => res.data.data),
  create: (data: any) => api.post<ApiResponse<Warehouse>>('/warehouses', data).then((res) => res.data.data),
  update: (id: number, data: any) => api.put<ApiResponse<Warehouse>>(`/warehouses/${id}`, data).then((res) => res.data.data),
  delete: (id: number) => api.delete<ApiResponse<void>>(`/warehouses/${id}`).then((res) => res.data),
};

export const categoryApi = {
  getAll: () => api.get<ApiResponse<Category[]>>('/categories/all').then((res) => res.data.data),
  getPaged: (params?: any) => api.get<ApiResponse<PagedResponse<Category>>>('/categories', { params }).then((res) => res.data.data),
  getById: (id: number) => api.get<ApiResponse<Category>>(`/categories/${id}`).then((res) => res.data.data),
  create: (data: any) => api.post<ApiResponse<Category>>('/categories', data).then((res) => res.data.data),
  update: (id: number, data: any) => api.put<ApiResponse<Category>>(`/categories/${id}`, data).then((res) => res.data.data),
  delete: (id: number) => api.delete<ApiResponse<void>>(`/categories/${id}`).then((res) => res.data),
};

// Analytics Services
export const analyticsApi = {
  getForecast: (productId: number, algorithm = 'MOVING_AVERAGE', horizon = '14_DAYS') =>
    api.get<ApiResponse<DemandForecast>>(`/analytics/forecast/${productId}`, { params: { algorithm, horizon } }).then((res) => res.data.data),
  getAllForecasts: (horizon = '14_DAYS') =>
    api.get<ApiResponse<DemandForecast[]>>('/analytics/forecasts', { params: { horizon } }).then((res) => res.data.data),
  getStockoutRisks: (criticalOnly = false) =>
    api.get<ApiResponse<StockoutRisk[]>>('/analytics/stockout-risk', { params: { criticalOnly } }).then((res) => res.data.data),
  getProductStockoutRisk: (productId: number) =>
    api.get<ApiResponse<StockoutRisk>>(`/analytics/stockout-risk/${productId}`).then((res) => res.data.data),
  getReorderRecommendations: () =>
    api.get<ApiResponse<ReorderRecommendation[]>>('/analytics/reorder-recommendations').then((res) => res.data.data),
  getProductReorder: (productId: number) =>
    api.get<ApiResponse<ReorderRecommendation>>(`/analytics/reorder-recommendations/${productId}`).then((res) => res.data.data),
  getAnomalies: () =>
    api.get<ApiResponse<AnomalyDetection[]>>('/analytics/anomalies').then((res) => res.data.data),
  analyzeTransaction: (transactionId: number) =>
    api.get<ApiResponse<AnomalyDetection>>(`/analytics/anomalies/transaction/${transactionId}`).then((res) => res.data.data),
};

// AI Services
export const aiApi = {
  getDailyBrief: () => api.get<ApiResponse<AIDailyBrief>>('/ai/daily-brief').then((res) => res.data.data),
  chat: (data: { message: string; conversationId?: string }) =>
    api.post<ApiResponse<AIChatResponse>>('/ai/chat', data).then((res) => res.data.data),
  explainProduct: (productId: number) =>
    api.get<ApiResponse<AIChatResponse>>(`/ai/explain/${productId}`).then((res) => res.data.data),
};

// System Settings Service
export const settingsApi = {
  getConfig: () => api.get<ApiResponse<SystemConfigResponse>>('/settings/config').then((res) => res.data.data),
};

// Audit Logs Service
export const auditLogApi = {
  getPaged: (params?: { action?: string; entityType?: string; page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<AuditLog>>>('/audit-logs', { params }).then((res) => res.data.data),
};

// User Administration Service
export const userApi = {
  getAll: () => api.get<ApiResponse<User[]>>('/users').then((res) => res.data.data),
  updateRole: (id: number, role: string) =>
    api.put<ApiResponse<User>>(`/users/${id}/role`, { role }).then((res) => res.data.data),
  updateStatus: (id: number, enabled: boolean) =>
    api.put<ApiResponse<User>>(`/users/${id}/status`, { enabled }).then((res) => res.data.data),
};

export default api;
