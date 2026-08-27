import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Boxes,
  Package,
  DollarSign,
  AlertTriangle,
  XCircle,
  ShoppingCart,
  ShieldAlert,
  Search,
  ArrowUpRight,
  TrendingUp,
  Bot,
  Warehouse,
  PackagePlus,
  ArrowLeftRight,
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
  Legend,
  PieChart,
  Pie,
  Cell,
} from 'recharts';
import { dashboardApi } from '../services/api';
import { StatCard } from '../components/common/StatCard';
import { Badge } from '../components/common/Badge';

const COLORS = ['#0ea5e9', '#6366f1', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6'];

export const DashboardPage: React.FC = () => {
  const navigate = useNavigate();

  const { data: summary, isLoading, error } = useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: dashboardApi.getSummary,
    refetchInterval: 15000,
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-4 border-sky-500 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-sm text-slate-400">Loading Authoritative Dashboard State...</p>
        </div>
      </div>
    );
  }

  if (error || !summary) {
    return (
      <div className="p-6 bg-rose-500/10 border border-rose-500/20 rounded-2xl text-rose-400">
        <p className="font-semibold">Unable to fetch dashboard metrics</p>
        <p className="text-xs text-rose-300 mt-1">Please ensure backend service is running.</p>
      </div>
    );
  }

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Header Banner */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-slate-900 via-slate-900 to-indigo-950/40 border border-slate-800 p-6 sm:p-8">
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-sky-500/10 border border-sky-500/30 text-sky-400 text-xs font-semibold mb-3">
              <Bot className="w-3.5 h-3.5" />
              <span>StockPilot AI Control Hub</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">
              Enterprise Inventory Overview
            </h2>
            <p className="text-sm text-slate-400 mt-1.5 max-w-2xl">
              Real-time synchronization across multi-hub fulfillment centers with deterministic statistical forecasting and invariant validation.
            </p>
          </div>
          <div className="flex items-center gap-3 shrink-0">
            <button
              onClick={() => navigate('/inventory')}
              className="flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white text-xs font-semibold border border-slate-700 transition-colors"
            >
              <PackagePlus className="w-4 h-4 text-sky-400" />
              <span>Manage Stock</span>
            </button>
            <button
              onClick={() => navigate('/ai-command-center')}
              className="flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white text-xs font-semibold shadow-lg shadow-sky-500/20 transition-all"
            >
              <Bot className="w-4 h-4" />
              <span>AI Brief &amp; Risks</span>
            </button>
          </div>
        </div>
      </div>

      {/* KPI Metric Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Active SKUs"
          value={summary.totalProducts.toLocaleString()}
          subtitle="Catalog breadth"
          icon={Boxes}
          colorScheme="blue"
          onClick={() => navigate('/products')}
        />
        <StatCard
          title="Total Physical On-Hand"
          value={`${summary.totalInventoryUnits.toLocaleString()} units`}
          subtitle="Physical aggregate units"
          icon={Package}
          colorScheme="emerald"
          onClick={() => navigate('/inventory')}
        />
        <StatCard
          title="Inventory Valuation"
          value={`$${summary.totalInventoryValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`}
          subtitle="At weighted cost price"
          icon={DollarSign}
          colorScheme="indigo"
        />
        <StatCard
          title="Critical Risk SKUs"
          value={summary.criticalRiskCount.toLocaleString()}
          subtitle="<= 6 days buffer"
          icon={ShieldAlert}
          colorScheme="rose"
          onClick={() => navigate('/analytics')}
        />
      </div>

      {/* Sub-Metric Ribbon */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div
          onClick={() => navigate('/inventory?stockStatus=LOW_STOCK')}
          className="p-4 rounded-xl bg-slate-900/60 border border-slate-800/80 hover:border-amber-500/40 cursor-pointer transition-colors"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-400">Low Stock Alerts</span>
            <AlertTriangle className="w-4 h-4 text-amber-400" />
          </div>
          <p className="text-xl font-bold text-amber-400 mt-2">{summary.lowStockCount}</p>
        </div>

        <div
          onClick={() => navigate('/inventory?stockStatus=OUT_OF_STOCK')}
          className="p-4 rounded-xl bg-slate-900/60 border border-slate-800/80 hover:border-rose-500/40 cursor-pointer transition-colors"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-400">Out of Stock</span>
            <XCircle className="w-4 h-4 text-rose-400" />
          </div>
          <p className="text-xl font-bold text-rose-400 mt-2">{summary.outOfStockCount}</p>
        </div>

        <div
          onClick={() => navigate('/purchase-orders?status=PENDING')}
          className="p-4 rounded-xl bg-slate-900/60 border border-slate-800/80 hover:border-sky-500/40 cursor-pointer transition-colors"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-400">Pending POs</span>
            <ShoppingCart className="w-4 h-4 text-sky-400" />
          </div>
          <p className="text-xl font-bold text-sky-400 mt-2">{summary.pendingPurchaseOrdersCount}</p>
        </div>

        <div
          onClick={() => navigate('/analytics')}
          className="p-4 rounded-xl bg-slate-900/60 border border-slate-800/80 hover:border-purple-500/40 cursor-pointer transition-colors"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-400">Statistical Anomalies</span>
            <Search className="w-4 h-4 text-purple-400" />
          </div>
          <p className="text-xl font-bold text-purple-400 mt-2">{summary.anomalyCount}</p>
        </div>
      </div>

      {/* Main Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* 14-Day Stock Movement Area Chart */}
        <div className="lg:col-span-2 bg-slate-900/80 border border-slate-800/80 rounded-2xl p-6 shadow-xl backdrop-blur-md">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="text-base font-semibold text-white">14-Day Stock Movement Flow</h3>
              <p className="text-xs text-slate-400 mt-0.5">Physical units received vs fulfilled over time</p>
            </div>
            <div className="flex items-center gap-4 text-xs">
              <span className="flex items-center gap-1.5 text-emerald-400">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-400"></span> Stock IN
              </span>
              <span className="flex items-center gap-1.5 text-rose-400">
                <span className="w-2.5 h-2.5 rounded-full bg-rose-400"></span> Stock OUT
              </span>
            </div>
          </div>
          <div className="h-72 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={summary.recentStockMovement}>
                <defs>
                  <linearGradient id="colorIn" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#10b981" stopOpacity={0.0} />
                  </linearGradient>
                  <linearGradient id="colorOut" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#f43f5e" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#f43f5e" stopOpacity={0.0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="date" stroke="#64748b" tick={{ fontSize: 11 }} />
                <YAxis stroke="#64748b" tick={{ fontSize: 11 }} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '0.75rem', color: '#fff', fontSize: '12px' }}
                />
                <Area type="monotone" dataKey="stockIn" stroke="#10b981" strokeWidth={2} fillOpacity={1} fill="url(#colorIn)" name="Stock IN" />
                <Area type="monotone" dataKey="stockOut" stroke="#f43f5e" strokeWidth={2} fillOpacity={1} fill="url(#colorOut)" name="Stock OUT" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Valuation by Category Chart */}
        <div className="bg-slate-900/80 border border-slate-800/80 rounded-2xl p-6 shadow-xl backdrop-blur-md flex flex-col justify-between">
          <div>
            <h3 className="text-base font-semibold text-white">Valuation by Category</h3>
            <p className="text-xs text-slate-400 mt-0.5">Capital distribution across inventory segments</p>
          </div>
          <div className="h-56 w-full my-auto">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={summary.inventoryValueByCategory}
                  dataKey="totalValue"
                  nameKey="categoryName"
                  cx="50%"
                  cy="50%"
                  innerRadius={50}
                  outerRadius={75}
                  paddingAngle={4}
                >
                  {summary.inventoryValueByCategory.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip
                  formatter={(val: any) => [`$${Number(val).toLocaleString()}`, 'Valuation']}
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '0.75rem', color: '#fff', fontSize: '12px' }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="space-y-1.5 pt-2 border-t border-slate-800/60 max-h-32 overflow-y-auto">
            {summary.inventoryValueByCategory.map((cat, idx) => (
              <div key={cat.categoryName} className="flex items-center justify-between text-xs">
                <span className="flex items-center gap-2 text-slate-300">
                  <span className="w-2 h-2 rounded-full" style={{ backgroundColor: COLORS[idx % COLORS.length] }}></span>
                  <span className="truncate max-w-[120px]">{cat.categoryName}</span>
                </span>
                <span className="font-medium text-white">${Number(cat.totalValue).toLocaleString(undefined, { maximumFractionDigits: 0 })}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Warehouse Distribution & Top Moving Products */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Warehouse Stock Matrix */}
        <div className="bg-slate-900/80 border border-slate-800/80 rounded-2xl p-6 shadow-xl backdrop-blur-md">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="text-base font-semibold text-white">Warehouse Inventory Breakdown</h3>
              <p className="text-xs text-slate-400 mt-0.5">Authoritative available vs reserved vs quarantined stock</p>
            </div>
            <Warehouse className="w-5 h-5 text-sky-400" />
          </div>
          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={summary.inventoryByWarehouse}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="warehouseCode" stroke="#64748b" tick={{ fontSize: 11 }} />
                <YAxis stroke="#64748b" tick={{ fontSize: 11 }} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '0.75rem', color: '#fff', fontSize: '12px' }}
                />
                <Legend wrapperStyle={{ fontSize: '11px' }} />
                <Bar dataKey="availableUnits" name="Available" fill="#0ea5e9" radius={[4, 4, 0, 0]} />
                <Bar dataKey="reservedUnits" name="Reserved" fill="#f59e0b" radius={[4, 4, 0, 0]} />
                <Bar dataKey="damagedUnits" name="Damaged" fill="#f43f5e" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Top Moving Products */}
        <div className="bg-slate-900/80 border border-slate-800/80 rounded-2xl p-6 shadow-xl backdrop-blur-md flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="text-base font-semibold text-white">Top Moving Velocity Products</h3>
                <p className="text-xs text-slate-400 mt-0.5">Highest transaction frequency and throughput</p>
              </div>
              <TrendingUp className="w-5 h-5 text-emerald-400" />
            </div>

            <div className="divide-y divide-slate-800/60">
              {summary.topMovingProducts.map((p, index) => (
                <div key={p.productId} className="py-3 flex items-center justify-between hover:bg-slate-800/30 px-2 rounded-xl transition-colors">
                  <div className="flex items-center gap-3">
                    <span className="w-6 h-6 rounded-lg bg-slate-800 text-slate-300 font-bold text-xs flex items-center justify-center border border-slate-700">
                      {index + 1}
                    </span>
                    <div>
                      <p className="text-sm font-semibold text-white truncate max-w-[200px]">{p.name}</p>
                      <p className="text-xs text-slate-400 font-mono">{p.sku}</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-bold text-sky-400">{p.totalQuantityMoved.toLocaleString()} units</p>
                    <p className="text-[11px] text-slate-400">{p.totalTransactions} events</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <button
            onClick={() => navigate('/transactions')}
            className="w-full mt-4 py-2.5 rounded-xl bg-slate-800/80 hover:bg-slate-800 text-slate-300 hover:text-white text-xs font-semibold border border-slate-700/60 flex items-center justify-center gap-1.5 transition-colors"
          >
            <span>View Complete Transaction Ledger</span>
            <ArrowUpRight className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    </div>
  );
};
