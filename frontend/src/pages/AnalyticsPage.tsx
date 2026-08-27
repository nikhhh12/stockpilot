import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  TrendingUp,
  AlertTriangle,
  ShoppingCart,
  Search,
  Sliders,
  ShieldAlert,
  ArrowUpRight,
  Sparkles,
  Info,
  Calendar,
  Building2,
  CheckCircle2,
} from 'lucide-react';
import {
  AreaChart,
  Area,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
} from 'recharts';
import { analyticsApi, productApi, purchaseOrderApi, warehouseApi } from '../services/api';
import { DemandForecast, StockoutRisk, ReorderRecommendation, AnomalyDetection, Product } from '../types';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';

export const AnalyticsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'FORECAST' | 'RISK' | 'REORDER' | 'ANOMALIES'>('FORECAST');

  // Forecast state
  const [selectedProductId, setSelectedProductId] = useState<number>(1);
  const [selectedAlgorithm, setSelectedAlgorithm] = useState<'MOVING_AVERAGE' | 'EXPONENTIAL_SMOOTHING'>('MOVING_AVERAGE');
  const [selectedHorizon, setSelectedHorizon] = useState<string>('14_DAYS');

  // Reorder 1-click PO creation modal
  const [reorderModalRec, setReorderModalRec] = useState<ReorderRecommendation | null>(null);

  // Queries
  const { data: products } = useQuery({ queryKey: ['products-all'], queryFn: productApi.getAll });
  const { data: warehouses } = useQuery({ queryKey: ['warehouses-all'], queryFn: warehouseApi.getAll });

  const { data: forecast, isLoading: isForecastLoading } = useQuery({
    queryKey: ['forecast', selectedProductId, selectedAlgorithm, selectedHorizon],
    queryFn: () => analyticsApi.getForecast(selectedProductId, selectedAlgorithm, selectedHorizon),
    enabled: !!selectedProductId,
  });

  const { data: risks, isLoading: isRisksLoading } = useQuery({
    queryKey: ['stockout-risks'],
    queryFn: () => analyticsApi.getStockoutRisks(false),
  });

  const { data: reorderRecs, isLoading: isReordersLoading } = useQuery({
    queryKey: ['reorder-recommendations'],
    queryFn: analyticsApi.getReorderRecommendations,
  });

  const { data: anomalies, isLoading: isAnomaliesLoading } = useQuery({
    queryKey: ['anomalies'],
    queryFn: analyticsApi.getAnomalies,
  });

  // Reorder PO Creation Mutation
  const createPOMutation = useMutation({
    mutationFn: purchaseOrderApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['purchase-orders'] });
      queryClient.invalidateQueries({ queryKey: ['reorder-recommendations'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      setReorderModalRec(null);
      alert('Purchase Order successfully generated from statistical reorder recommendation!');
    },
    onError: (err: any) => alert(err.response?.data?.message || err.message),
  });

  const handleGeneratePO = (rec: ReorderRecommendation) => {
    setReorderModalRec(rec);
  };

  const confirmGeneratePO = () => {
    if (!reorderModalRec) return;
    const prod = products?.find((p) => p.id === reorderModalRec.productId);
    createPOMutation.mutate({
      supplierId: reorderModalRec.preferredSupplierId,
      warehouseId: reorderModalRec.warehouseId || warehouses?.[0]?.id || 1,
      expectedDeliveryDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      notes: `Generated automatically via StockPilot Statistical Reorder Engine: ${reorderModalRec.reason}`,
      items: [
        {
          productId: reorderModalRec.productId,
          orderedQuantity: reorderModalRec.recommendedReorderQuantity,
          unitCost: prod ? prod.costPrice : 100,
        },
      ],
    });
  };

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-white tracking-tight flex items-center gap-2.5">
            <TrendingUp className="w-6 h-6 text-sky-400" />
            <span>Statistical Intelligence &amp; Forecasting</span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Standardized time-series forecasting, run-rate risk horizons, replenishment optimization, and Z-score anomaly detection.
          </p>
        </div>
      </div>

      {/* Tabs Navigation */}
      <div className="flex items-center gap-2 border-b border-slate-800 pb-2">
        <button
          onClick={() => setActiveTab('FORECAST')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold transition-all ${
            activeTab === 'FORECAST'
              ? 'bg-sky-500/20 text-sky-400 border border-sky-500/30'
              : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
          }`}
        >
          Demand Forecasting Engine
        </button>
        <button
          onClick={() => setActiveTab('RISK')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold transition-all ${
            activeTab === 'RISK'
              ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
              : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
          }`}
        >
          Stockout Risk Matrix
        </button>
        <button
          onClick={() => setActiveTab('REORDER')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold transition-all ${
            activeTab === 'REORDER'
              ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
              : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
          }`}
        >
          Replenishment Recommendations
        </button>
        <button
          onClick={() => setActiveTab('ANOMALIES')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold transition-all ${
            activeTab === 'ANOMALIES'
              ? 'bg-purple-500/20 text-purple-400 border border-purple-500/30'
              : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
          }`}
        >
          Z-Score Anomaly Radar
        </button>
      </div>

      {/* Tab 1: Demand Forecasting Engine */}
      {activeTab === 'FORECAST' && (
        <div className="space-y-6">
          {/* Controls Bar */}
          <div className="p-4 bg-slate-900/80 border border-slate-800 rounded-2xl flex flex-col md:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-3 w-full md:w-auto">
              <label className="text-xs text-slate-400 shrink-0">Product SKU:</label>
              <select
                value={selectedProductId}
                onChange={(e) => setSelectedProductId(Number(e.target.value))}
                className="bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-sky-500 min-w-[220px]"
              >
                {products?.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} ({p.sku})
                  </option>
                ))}
              </select>
            </div>

            <div className="flex items-center gap-3 w-full md:w-auto">
              {/* Algorithm Toggle */}
              <div className="flex items-center bg-slate-950 p-1 rounded-xl border border-slate-800 text-xs">
                <button
                  onClick={() => setSelectedAlgorithm('MOVING_AVERAGE')}
                  className={`px-3 py-1 rounded-lg font-medium transition-colors ${
                    selectedAlgorithm === 'MOVING_AVERAGE'
                      ? 'bg-sky-500/20 text-sky-400 border border-sky-500/30 font-semibold'
                      : 'text-slate-400 hover:text-white'
                  }`}
                >
                  Moving Average (SMA-14)
                </button>
                <button
                  onClick={() => setSelectedAlgorithm('EXPONENTIAL_SMOOTHING')}
                  className={`px-3 py-1 rounded-lg font-medium transition-colors ${
                    selectedAlgorithm === 'EXPONENTIAL_SMOOTHING'
                      ? 'bg-sky-500/20 text-sky-400 border border-sky-500/30 font-semibold'
                      : 'text-slate-400 hover:text-white'
                  }`}
                >
                  Exponential Smoothing (α=0.3)
                </button>
              </div>

              {/* Horizon Select */}
              <select
                value={selectedHorizon}
                onChange={(e) => setSelectedHorizon(e.target.value)}
                className="bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-sky-500"
              >
                <option value="7_DAYS">7-Day Horizon</option>
                <option value="14_DAYS">14-Day Horizon</option>
                <option value="30_DAYS">30-Day Horizon</option>
              </select>
            </div>
          </div>

          {/* Forecast Output Card */}
          {isForecastLoading ? (
            <div className="p-12 text-center text-slate-400 bg-slate-900/60 rounded-2xl border border-slate-800">
              Running standardized time-series algorithm...
            </div>
          ) : forecast ? (
            <div className="space-y-6">
              {/* Top Stats Banner */}
              <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
                <div className="p-4 bg-slate-900/80 border border-slate-800 rounded-xl">
                  <span className="text-slate-400 text-xs">Forecast Status</span>
                  <div className="mt-1">
                    <Badge variant={forecast.status === 'OK' ? 'success' : 'warning'}>{forecast.status}</Badge>
                  </div>
                </div>

                <div className="p-4 bg-slate-900/80 border border-slate-800 rounded-xl">
                  <span className="text-slate-400 text-xs">Historical Run Rate</span>
                  <p className="text-xl font-bold text-sky-400 mt-1">{forecast.averageDailyDemand} units/day</p>
                </div>

                <div className="p-4 bg-slate-900/80 border border-slate-800 rounded-xl">
                  <span className="text-slate-400 text-xs">Projected Demand ({forecast.forecastHorizon})</span>
                  <p className="text-xl font-bold text-emerald-400 mt-1">{forecast.predictedDemand} units</p>
                </div>

                <div className="p-4 bg-slate-900/80 border border-slate-800 rounded-xl">
                  <span className="text-slate-400 text-xs">Baseline Data Points</span>
                  <p className="text-xl font-bold text-white mt-1">{forecast.dataPointsUsed} days</p>
                </div>
              </div>

              {/* Message Note */}
              <div className="p-3 bg-slate-900/60 border border-slate-800 rounded-xl text-xs text-slate-300 flex items-center gap-2">
                <Info className="w-4 h-4 text-sky-400 shrink-0" />
                <span>{forecast.message}</span>
              </div>

              {/* Historical Consumption Chart */}
              <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6 shadow-xl">
                <h3 className="text-sm font-semibold text-white mb-4">
                  Historical Daily Consumption Series (30-Day Window)
                </h3>
                <div className="h-72 w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={forecast.historicalData}>
                      <defs>
                        <linearGradient id="forecastFill" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="#0ea5e9" stopOpacity={0.4} />
                          <stop offset="95%" stopColor="#0ea5e9" stopOpacity={0.0} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                      <XAxis dataKey="date" stroke="#64748b" tick={{ fontSize: 10 }} />
                      <YAxis stroke="#64748b" tick={{ fontSize: 10 }} />
                      <Tooltip
                        contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '0.75rem', color: '#fff', fontSize: '12px' }}
                      />
                      <Area
                        type="monotone"
                        dataKey="quantity"
                        name="Demand (Units)"
                        stroke="#0ea5e9"
                        strokeWidth={2}
                        fill="url(#forecastFill)"
                      />
                    </AreaChart>
                  </ResponsiveContainer>
                </div>
              </div>
            </div>
          ) : null}
        </div>
      )}

      {/* Tab 2: Stockout Risk Matrix */}
      {activeTab === 'RISK' && (
        <div className="space-y-6">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div className="p-4 bg-rose-500/10 border border-rose-500/30 rounded-xl">
              <span className="text-xs text-rose-400 font-semibold uppercase tracking-wider">CRITICAL RISK</span>
              <p className="text-2xl font-bold text-white mt-1">
                {risks?.filter((r) => r.riskLevel === 'CRITICAL').length || 0}
              </p>
              <p className="text-[11px] text-rose-300 mt-1">&le; 6 days coverage</p>
            </div>

            <div className="p-4 bg-amber-500/10 border border-amber-500/30 rounded-xl">
              <span className="text-xs text-amber-400 font-semibold uppercase tracking-wider">HIGH RISK</span>
              <p className="text-2xl font-bold text-white mt-1">
                {risks?.filter((r) => r.riskLevel === 'HIGH').length || 0}
              </p>
              <p className="text-[11px] text-amber-300 mt-1">7 &ndash; 14 days coverage</p>
            </div>

            <div className="p-4 bg-sky-500/10 border border-sky-500/30 rounded-xl">
              <span className="text-xs text-sky-400 font-semibold uppercase tracking-wider">MEDIUM RISK</span>
              <p className="text-2xl font-bold text-white mt-1">
                {risks?.filter((r) => r.riskLevel === 'MEDIUM').length || 0}
              </p>
              <p className="text-[11px] text-sky-300 mt-1">15 &ndash; 30 days coverage</p>
            </div>

            <div className="p-4 bg-emerald-500/10 border border-emerald-500/30 rounded-xl">
              <span className="text-xs text-emerald-400 font-semibold uppercase tracking-wider">LOW RISK</span>
              <p className="text-2xl font-bold text-white mt-1">
                {risks?.filter((r) => r.riskLevel === 'LOW').length || 0}
              </p>
              <p className="text-[11px] text-emerald-300 mt-1">&gt; 30 days coverage</p>
            </div>
          </div>

          {/* Risk Table */}
          <div className="bg-slate-900/80 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="border-b border-slate-800 bg-slate-900/50 text-slate-400 font-medium">
                    <th className="p-4">SKU &amp; Product</th>
                    <th className="p-4 text-center">Usable Stock</th>
                    <th className="p-4 text-center">Daily Run Rate</th>
                    <th className="p-4 text-center">Days Remaining</th>
                    <th className="p-4 text-center">Risk Level</th>
                    <th className="p-4">Risk Diagnosis &amp; Root Cause</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {isRisksLoading ? (
                    <tr>
                      <td colSpan={6} className="p-8 text-center text-slate-400">
                        Evaluating risk horizons...
                      </td>
                    </tr>
                  ) : risks?.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="p-8 text-center text-slate-400">
                        No risk conditions identified.
                      </td>
                    </tr>
                  ) : (
                    risks?.map((r) => (
                      <tr key={r.productId} className="hover:bg-slate-800/30 transition-colors">
                        <td className="p-4">
                          <div className="font-semibold text-white">{r.productName}</div>
                          <div className="text-[11px] font-mono text-sky-400">{r.productSku}</div>
                        </td>
                        <td className="p-4 text-center font-bold text-white">{r.availableQuantity} units</td>
                        <td className="p-4 text-center text-slate-300">{r.averageDailyDemand} / day</td>
                        <td className="p-4 text-center font-mono font-bold text-sm">
                          {r.estimatedDaysRemaining > 900 ? '999+' : `${r.estimatedDaysRemaining} days`}
                        </td>
                        <td className="p-4 text-center">
                          <Badge
                            variant={
                              r.riskLevel === 'CRITICAL'
                                ? 'danger'
                                : r.riskLevel === 'HIGH'
                                ? 'warning'
                                : r.riskLevel === 'MEDIUM'
                                ? 'info'
                                : 'success'
                            }
                          >
                            {r.riskLevel}
                          </Badge>
                        </td>
                        <td className="p-4 text-slate-300 max-w-md">{r.explanation}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Tab 3: Replenishment Recommendations */}
      {activeTab === 'REORDER' && (
        <div className="space-y-6">
          <div className="p-4 bg-slate-900/80 border border-slate-800 rounded-2xl flex items-center justify-between">
            <div>
              <h3 className="text-sm font-semibold text-white">Automated Replenishment Optimization</h3>
              <p className="text-xs text-slate-400 mt-0.5">
                Evaluates physical available stock, reserved orders, and on-order purchase pipeline against safety thresholds.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {isReordersLoading ? (
              <div className="col-span-2 p-8 text-center text-slate-400">Loading recommendations...</div>
            ) : reorderRecs?.length === 0 ? (
              <div className="col-span-2 p-8 text-center text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 rounded-2xl">
                All catalog inventory levels meet optimal safety stock buffers. No reorders needed!
              </div>
            ) : (
              reorderRecs?.map((rec) => (
                <div
                  key={rec.productId}
                  className="p-5 bg-slate-900/80 border border-slate-800 rounded-2xl space-y-4 shadow-xl hover:border-slate-700 transition-colors"
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <h4 className="font-semibold text-white text-sm">{rec.productName}</h4>
                      <p className="text-xs font-mono text-sky-400 mt-0.5">{rec.productSku}</p>
                    </div>
                    <Badge variant={rec.urgency === 'CRITICAL' ? 'danger' : rec.urgency === 'HIGH' ? 'warning' : 'info'}>
                      {rec.urgency} Urgency
                    </Badge>
                  </div>

                  <p className="text-xs text-slate-300 leading-relaxed">{rec.reason}</p>

                  <div className="grid grid-cols-3 gap-2 p-3 bg-slate-950/80 rounded-xl text-xs">
                    <div>
                      <span className="text-slate-500 text-[10px]">Current Stock</span>
                      <p className="font-bold text-white">{rec.availableStock} units</p>
                    </div>
                    <div>
                      <span className="text-slate-500 text-[10px]">Pipeline On-Order</span>
                      <p className="font-bold text-sky-400">{rec.pipelineOnOrderQuantity} units</p>
                    </div>
                    <div>
                      <span className="text-slate-500 text-[10px]">Safety Buffer</span>
                      <p className="font-bold text-amber-400">{rec.safetyStock} units</p>
                    </div>
                  </div>

                  <div className="flex items-center justify-between pt-2 border-t border-slate-800">
                    <div>
                      <span className="text-[10px] text-slate-400 block">Suggested PO</span>
                      <span className="font-bold text-emerald-400 text-sm">{rec.recommendedReorderQuantity} units</span>
                      <span className="text-slate-400 text-xs ml-1.5 font-mono">(${rec.estimatedCost.toFixed(2)})</span>
                    </div>

                    <button
                      onClick={() => handleGeneratePO(rec)}
                      className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-sky-500 hover:bg-sky-400 text-white font-semibold text-xs shadow-md shadow-sky-500/20 transition-all hover:scale-105"
                    >
                      <ShoppingCart className="w-3.5 h-3.5" />
                      <span>1-Click Generate PO</span>
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* Tab 4: Z-Score Anomaly Radar */}
      {activeTab === 'ANOMALIES' && (
        <div className="space-y-6">
          <div className="p-4 bg-slate-900/80 border border-slate-800 rounded-2xl flex items-center justify-between">
            <div>
              <h3 className="text-sm font-semibold text-white">Statistical Z-Score Anomaly Radar</h3>
              <p className="text-xs text-slate-400 mt-0.5">
                Exact Formula: <code>absoluteZScore = |X - &mu;| / &sigma;</code> (Safe <code>NO_VARIANCE</code> return when &sigma; = 0)
              </p>
            </div>
          </div>

          <div className="bg-slate-900/80 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="border-b border-slate-800 bg-slate-900/50 text-slate-400 font-medium">
                    <th className="p-4">Tx ID &amp; Date</th>
                    <th className="p-4">SKU &amp; Product</th>
                    <th className="p-4">Warehouse</th>
                    <th className="p-4 text-center">Observed Qty</th>
                    <th className="p-4 text-center">Mean &amp; &sigma;</th>
                    <th className="p-4 text-center">Calculated Z-Score</th>
                    <th className="p-4 text-center">Classification</th>
                    <th className="p-4">Statistical Reason</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {isAnomaliesLoading ? (
                    <tr>
                      <td colSpan={8} className="p-8 text-center text-slate-400">
                        Scanning transaction ledger for statistical outliers...
                      </td>
                    </tr>
                  ) : anomalies?.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="p-8 text-center text-slate-400">
                        No transactions registered yet.
                      </td>
                    </tr>
                  ) : (
                    anomalies?.map((a) => (
                      <tr key={a.transactionId} className="hover:bg-slate-800/30 transition-colors">
                        <td className="p-4 font-mono text-slate-400">
                          <div className="font-semibold text-white">#{a.transactionId}</div>
                          <div className="text-[10px] text-slate-500">{new Date(a.transactionDate).toLocaleDateString()}</div>
                        </td>
                        <td className="p-4">
                          <div className="font-semibold text-white">{a.productName}</div>
                          <div className="text-[11px] font-mono text-sky-400">{a.productSku}</div>
                        </td>
                        <td className="p-4 text-slate-300">{a.warehouseName}</td>
                        <td className="p-4 text-center font-bold text-white text-sm">{a.observedQuantity} units</td>
                        <td className="p-4 text-center font-mono text-slate-400">
                          &mu;={a.expectedMeanQuantity} | &sigma;={a.standardDeviation}
                        </td>
                        <td className="p-4 text-center font-mono font-bold text-sm">
                          {a.absoluteZScore !== null ? (
                            <span className={a.absoluteZScore >= 2.0 ? 'text-purple-400' : 'text-slate-400'}>
                              {a.absoluteZScore.toFixed(2)}
                            </span>
                          ) : (
                            <span className="text-slate-500">&mdash;</span>
                          )}
                        </td>
                        <td className="p-4 text-center">
                          <Badge
                            variant={
                              a.status === 'STATISTICAL_ANOMALY'
                                ? 'danger'
                                : a.status === 'NO_VARIANCE'
                                ? 'purple'
                                : a.status === 'INSUFFICIENT_DATA'
                                ? 'warning'
                                : 'neutral'
                            }
                          >
                            {a.status}
                          </Badge>
                        </td>
                        <td className="p-4 text-slate-300 max-w-sm">{a.reason}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Prefilled PO Generation Confirmation Modal */}
      {reorderModalRec && (
        <Modal
          isOpen={!!reorderModalRec}
          onClose={() => setReorderModalRec(null)}
          title="Confirm Auto-Generated Purchase Order"
          subtitle={`Product: ${reorderModalRec.productName} (${reorderModalRec.productSku})`}
          maxWidth="md"
        >
          <div className="space-y-4 text-xs">
            <p className="text-slate-300">
              You are about to issue a Purchase Order in <strong>PENDING</strong> status based on calculated depletion velocity:
            </p>

            <div className="p-3.5 bg-slate-950 rounded-xl border border-slate-800 space-y-2 font-mono">
              <div className="flex justify-between">
                <span className="text-slate-400">Supplier:</span>
                <span className="text-white font-bold">{reorderModalRec.preferredSupplierName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Suggested Quantity:</span>
                <span className="text-sky-400 font-bold">{reorderModalRec.recommendedReorderQuantity} units</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Estimated Cost:</span>
                <span className="text-emerald-400 font-bold">${reorderModalRec.estimatedCost.toFixed(2)}</span>
              </div>
            </div>

            <div className="pt-4 border-t border-slate-800 flex justify-end gap-3">
              <button
                onClick={() => setReorderModalRec(null)}
                className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 hover:bg-slate-700"
              >
                Cancel
              </button>
              <button
                onClick={confirmGeneratePO}
                disabled={createPOMutation.isPending}
                className="px-4 py-2 rounded-xl bg-sky-500 hover:bg-sky-400 text-white font-semibold shadow-lg shadow-sky-500/20 flex items-center gap-1.5"
              >
                <CheckCircle2 className="w-4 h-4" />
                <span>{createPOMutation.isPending ? 'Generating...' : 'Confirm & Create PO'}</span>
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
