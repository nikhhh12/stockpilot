// Core Domain & API Types for StockPilot AI

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  errorCode?: string;
  timestamp: string;
}

export interface PagedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export type Role = 'ADMIN' | 'INVENTORY_MANAGER' | 'STAFF';

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
  enabled: boolean;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: User;
}

export interface Category {
  id: number;
  name: string;
  description?: string;
  active: boolean;
  createdAt: string;
}

export interface Supplier {
  id: number;
  companyName: string;
  contactPerson?: string;
  email?: string;
  phone?: string;
  address?: string;
  taxIdentifier?: string;
  defaultLeadTimeDays: number;
  active: boolean;
  createdAt: string;
}

export interface Warehouse {
  id: number;
  name: string;
  code: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  active: boolean;
  createdAt: string;
}

export interface Product {
  id: number;
  sku: string;
  name: string;
  description?: string;
  category: Category;
  supplier: Supplier;
  costPrice: number;
  sellingPrice: number;
  unit: string;
  reorderLevel: number;
  reorderQuantity: number;
  active: boolean;
  totalAvailableQuantity: number;
  totalReservedQuantity: number;
  totalDamagedQuantity: number;
  totalOnHandQuantity: number;
  createdAt: string;
  updatedAt?: string;
}

export interface Inventory {
  id: number;
  product: Product;
  warehouse: Warehouse;
  availableQuantity: number;
  reservedQuantity: number;
  damagedQuantity: number;
  onHandQuantity: number;
  calculatedStockoutRisk?: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  version: number;
  updatedAt: string;
}

export type TransactionType =
  | 'STOCK_IN'
  | 'STOCK_OUT'
  | 'ADJUSTMENT'
  | 'RESERVE'
  | 'RELEASE'
  | 'TRANSFER_IN'
  | 'TRANSFER_OUT'
  | 'RETURN'
  | 'DAMAGE';

export interface StockTransaction {
  id: number;
  productId?: number;
  productSku?: string;
  productName?: string;
  warehouseId?: number;
  warehouseName?: string;
  product?: Product;
  warehouse?: Warehouse;
  transactionType: TransactionType;
  quantity: number;
  previousAvailableQuantity: number;
  newAvailableQuantity: number;
  previousReservedQuantity: number;
  newReservedQuantity: number;
  previousDamagedQuantity: number;
  newDamagedQuantity: number;
  previousQuantity?: number;
  newQuantity?: number;
  reason?: string;
  referenceNumber?: string;
  notes?: string;
  performedById?: number;
  performedByName?: string;
  performedBy?: User;
  createdAt: string;
}

export type PurchaseOrderStatus =
  | 'DRAFT'
  | 'PENDING'
  | 'APPROVED'
  | 'ORDERED'
  | 'PARTIALLY_RECEIVED'
  | 'RECEIVED'
  | 'CANCELLED';

export interface PurchaseOrderItem {
  id: number;
  product: Product;
  orderedQuantity: number;
  receivedQuantity: number;
  unitCost: number;
  totalCost: number;
}

export interface PurchaseOrder {
  id: number;
  poNumber: string;
  supplier: Supplier;
  warehouse: Warehouse;
  status: PurchaseOrderStatus;
  orderDate: string;
  expectedDeliveryDate?: string;
  receivedDate?: string;
  totalAmount: number;
  notes?: string;
  createdBy: User;
  items: PurchaseOrderItem[];
  createdAt: string;
  updatedAt?: string;
}

// Statistical Intelligence Types
export interface DemandForecast {
  productId: number;
  productSku: string;
  productName: string;
  forecastHorizon: string;
  averageDailyDemand: number;
  predictedDemand: number;
  algorithm: 'MOVING_AVERAGE' | 'EXPONENTIAL_SMOOTHING';
  dataPointsUsed: number;
  status: 'OK' | 'INSUFFICIENT_DATA';
  message: string;
  historicalData: Array<{ date: string; quantity: number }>;
}

export interface StockoutRisk {
  productId: number;
  productSku: string;
  productName: string;
  availableQuantity: number;
  averageDailyDemand: number;
  estimatedDaysRemaining: number;
  reorderLevel: number;
  riskLevel: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  explanation: string;
}

export interface ReorderRecommendation {
  productId: number;
  productSku: string;
  productName: string;
  preferredSupplierId: number;
  preferredSupplierName: string;
  warehouseId: number;
  warehouseName: string;
  availableStock: number;
  reservedStock: number;
  pipelineOnOrderQuantity: number;
  averageDailyDemand: number;
  safetyStock: number;
  recommendedReorderQuantity: number;
  estimatedCost: number;
  urgency: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  reason: string;
}

export interface AnomalyDetection {
  transactionId: number;
  productId: number;
  productSku: string;
  productName: string;
  warehouseId: number;
  warehouseName: string;
  transactionType: string;
  observedQuantity: number;
  expectedMeanQuantity: number;
  standardDeviation: number;
  absoluteZScore: number | null;
  status: 'STATISTICAL_ANOMALY' | 'NORMAL' | 'NO_VARIANCE' | 'INSUFFICIENT_DATA';
  severity: 'HIGH' | 'MEDIUM' | 'LOW';
  reason: string;
  transactionDate: string;
}

// Generative AI Types
export interface AIDailyBrief {
  facts: {
    totalProducts: number;
    lowStockCount: number;
    outOfStockCount: number;
    criticalRiskCount: number;
    anomalyCount: number;
    pendingPurchaseOrdersCount: number;
  };
  calculations: {
    totalAvailableUnits: number;
    totalReservedUnits: number;
    totalDamagedUnits: number;
    totalOnHandUnits: number;
    totalInventoryValue: number;
  };
  aiInterpretation: {
    executiveSummary: string;
    highPriorityRisks: string[];
    recommendedActions: string[];
  };
  providerAvailable: boolean;
  generatedAt: string;
}

export interface AIChatResponse {
  reply: string;
  conversationId: string;
  toolsInvoked: string[];
  providerAvailable: boolean;
  timestamp: string;
}

// Dashboard Contract
export interface DashboardSummary {
  totalProducts: number;
  totalInventoryUnits: number;
  totalInventoryValue: number;
  lowStockCount: number;
  outOfStockCount: number;
  pendingPurchaseOrdersCount: number;
  criticalRiskCount: number;
  anomalyCount: number;
  inventoryValueByCategory: Array<{ categoryName: string; totalUnits: number; totalValue: number }>;
  inventoryByWarehouse: Array<{
    warehouseName: string;
    warehouseCode: string;
    availableUnits: number;
    reservedUnits: number;
    damagedUnits: number;
    onHandUnits: number;
  }>;
  recentStockMovement: Array<{ date: string; stockIn: number; stockOut: number }>;
  topMovingProducts: Array<{
    productId: number;
    sku: string;
    name: string;
    totalTransactions: number;
    totalQuantityMoved: number;
  }>;
}

export interface SystemConfigResponse {
  analytics: {
    lowRiskThresholdDays: number;
    mediumRiskThresholdDays: number;
    highRiskThresholdDays: number;
    criticalRiskThresholdDays: number;
    reorderSafetyFactor: number;
    defaultForecastingAlgorithm: string;
    supportedForecastHorizons: string[];
  };
  systemStatus: {
    databaseStatus: string;
    apiStatus: string;
    invariantsEnforced: string;
    optimisticLockingActive: boolean;
    aiProvider: string;
    aiModel: string;
    aiAvailable: boolean;
  };
}
