import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  PackageSearch,
  Plus,
  Minus,
  Sliders,
  BookmarkPlus,
  Undo2,
  AlertOctagon,
  RotateCcw,
  ArrowLeftRight,
  Warehouse,
  Search,
  Filter,
  ShieldCheck,
  AlertTriangle,
} from 'lucide-react';
import { inventoryApi, productApi, warehouseApi, categoryApi } from '../services/api';
import { Inventory, Product, Warehouse as WarehouseType } from '../types';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';

export const InventoryPage: React.FC = () => {
  const queryClient = useQueryClient();

  // Search & Filter State
  const [search, setSearch] = useState('');
  const [warehouseId, setWarehouseId] = useState<number | undefined>();
  const [categoryId, setCategoryId] = useState<number | undefined>();
  const [stockStatus, setStockStatus] = useState<string>('');
  const [page, setPage] = useState(0);

  // Modal Action Types: 'IN' | 'OUT' | 'ADJUST' | 'RESERVE' | 'DAMAGE' | 'RETURN' | 'TRANSFER'
  const [activeAction, setActiveAction] = useState<string | null>(null);
  const [selectedInventory, setSelectedInventory] = useState<Inventory | null>(null);

  // Form States
  const [actionForm, setActionForm] = useState({
    productId: 1,
    warehouseId: 1,
    sourceWarehouseId: 1,
    destinationWarehouseId: 2,
    quantity: 10,
    resultingQuantity: 10,
    referenceNumber: '',
    reason: '',
    notes: '',
  });

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Queries
  const { data: inventoryData, isLoading } = useQuery({
    queryKey: ['inventories', search, warehouseId, categoryId, stockStatus, page],
    queryFn: () =>
      inventoryApi.getPaged({
        search: search || undefined,
        warehouseId,
        categoryId,
        stockStatus: stockStatus || undefined,
        page,
        size: 10,
      }),
  });

  const { data: products } = useQuery({ queryKey: ['products-all'], queryFn: productApi.getAll });
  const { data: warehouses } = useQuery({ queryKey: ['warehouses-all'], queryFn: warehouseApi.getAll });
  const { data: categories } = useQuery({ queryKey: ['categories-all'], queryFn: categoryApi.getAll });

  // Generic mutation handler with 409 conflict handling & automatic query invalidation
  const handleMutationSuccess = (msg: string) => {
    queryClient.invalidateQueries({ queryKey: ['inventories'] });
    queryClient.invalidateQueries({ queryKey: ['products'] });
    queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
    queryClient.invalidateQueries({ queryKey: ['transactions'] });
    setActiveAction(null);
    setErrorMessage(null);
  };

  const handleMutationError = (err: any) => {
    if (err.response?.status === 409) {
      setErrorMessage('Inventory was updated by another user. Please refresh and retry.');
      queryClient.invalidateQueries({ queryKey: ['inventories'] });
    } else {
      setErrorMessage(err.response?.data?.message || err.message || 'Operation failed');
    }
  };

  const stockInMutation = useMutation({
    mutationFn: inventoryApi.stockIn,
    onSuccess: () => handleMutationSuccess('Stock IN recorded successfully'),
    onError: handleMutationError,
  });

  const stockOutMutation = useMutation({
    mutationFn: inventoryApi.stockOut,
    onSuccess: () => handleMutationSuccess('Stock OUT recorded successfully'),
    onError: handleMutationError,
  });

  const adjustMutation = useMutation({
    mutationFn: inventoryApi.adjust,
    onSuccess: () => handleMutationSuccess('Stock adjusted successfully'),
    onError: handleMutationError,
  });

  const reserveMutation = useMutation({
    mutationFn: inventoryApi.reserve,
    onSuccess: () => handleMutationSuccess('Stock reserved successfully'),
    onError: handleMutationError,
  });

  const damageMutation = useMutation({
    mutationFn: inventoryApi.damage,
    onSuccess: () => handleMutationSuccess('Stock marked as damaged'),
    onError: handleMutationError,
  });

  const returnMutation = useMutation({
    mutationFn: inventoryApi.returnStock,
    onSuccess: () => handleMutationSuccess('Returned stock added to available inventory'),
    onError: handleMutationError,
  });

  const transferMutation = useMutation({
    mutationFn: inventoryApi.transfer,
    onSuccess: () => handleMutationSuccess('Transfer completed atomically between warehouses'),
    onError: handleMutationError,
  });

  const openActionModal = (action: string, inv?: Inventory) => {
    setActiveAction(action);
    setErrorMessage(null);
    if (inv) {
      setSelectedInventory(inv);
      setActionForm({
        productId: inv.product.id,
        warehouseId: inv.warehouse.id,
        sourceWarehouseId: inv.warehouse.id,
        destinationWarehouseId: warehouses?.find((w) => w.id !== inv.warehouse.id)?.id || inv.warehouse.id,
        quantity: 5,
        resultingQuantity: inv.availableQuantity,
        referenceNumber: `REF-${Date.now().toString().slice(-6)}`,
        reason: '',
        notes: '',
      });
    } else {
      setSelectedInventory(null);
      setActionForm({
        productId: products?.[0]?.id || 1,
        warehouseId: warehouses?.[0]?.id || 1,
        sourceWarehouseId: warehouses?.[0]?.id || 1,
        destinationWarehouseId: warehouses?.[1]?.id || 2,
        quantity: 10,
        resultingQuantity: 10,
        referenceNumber: `REF-${Date.now().toString().slice(-6)}`,
        reason: '',
        notes: '',
      });
    }
  };

  const handleActionSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    switch (activeAction) {
      case 'IN':
        stockInMutation.mutate({
          productId: actionForm.productId,
          warehouseId: actionForm.warehouseId,
          quantity: Number(actionForm.quantity),
          reason: actionForm.reason,
          referenceNumber: actionForm.referenceNumber,
          notes: actionForm.notes,
        });
        break;
      case 'OUT':
        stockOutMutation.mutate({
          productId: actionForm.productId,
          warehouseId: actionForm.warehouseId,
          quantity: Number(actionForm.quantity),
          reason: actionForm.reason,
          referenceNumber: actionForm.referenceNumber,
          notes: actionForm.notes,
        });
        break;
      case 'ADJUST':
        adjustMutation.mutate({
          productId: actionForm.productId,
          warehouseId: actionForm.warehouseId,
          resultingQuantity: Number(actionForm.resultingQuantity),
          reason: actionForm.reason,
          referenceNumber: actionForm.referenceNumber,
          notes: actionForm.notes,
        });
        break;
      case 'RESERVE':
        reserveMutation.mutate({
          productId: actionForm.productId,
          warehouseId: actionForm.warehouseId,
          quantity: Number(actionForm.quantity),
          referenceNumber: actionForm.referenceNumber,
          notes: actionForm.notes,
        });
        break;
      case 'DAMAGE':
        damageMutation.mutate({
          productId: actionForm.productId,
          warehouseId: actionForm.warehouseId,
          quantity: Number(actionForm.quantity),
          reason: actionForm.reason,
          referenceNumber: actionForm.referenceNumber,
          notes: actionForm.notes,
        });
        break;
      case 'RETURN':
        returnMutation.mutate({
          productId: actionForm.productId,
          warehouseId: actionForm.warehouseId,
          quantity: Number(actionForm.quantity),
          reason: actionForm.reason,
          referenceNumber: actionForm.referenceNumber,
          notes: actionForm.notes,
        });
        break;
      case 'TRANSFER':
        transferMutation.mutate({
          productId: actionForm.productId,
          sourceWarehouseId: actionForm.sourceWarehouseId,
          destinationWarehouseId: actionForm.destinationWarehouseId,
          quantity: Number(actionForm.quantity),
          referenceNumber: actionForm.referenceNumber,
          notes: actionForm.notes,
        });
        break;
    }
  };

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-white tracking-tight flex items-center gap-2.5">
            <PackageSearch className="w-6 h-6 text-sky-400" />
            <span>Warehouse Inventory Engine</span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Authoritative transactional ledger preserving invariant: <code>onHand = available + reserved + damaged</code>
          </p>
        </div>

        {/* Global Action Toolbar */}
        <div className="flex items-center gap-2 flex-wrap">
          <button
            onClick={() => openActionModal('IN')}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 hover:bg-emerald-500/25 text-xs font-semibold"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Stock In</span>
          </button>
          <button
            onClick={() => openActionModal('OUT')}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-rose-500/15 text-rose-400 border border-rose-500/30 hover:bg-rose-500/25 text-xs font-semibold"
          >
            <Minus className="w-3.5 h-3.5" />
            <span>Stock Out</span>
          </button>
          <button
            onClick={() => openActionModal('TRANSFER')}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-sky-500/15 text-sky-400 border border-sky-500/30 hover:bg-sky-500/25 text-xs font-semibold"
          >
            <ArrowLeftRight className="w-3.5 h-3.5" />
            <span>Transfer</span>
          </button>
          <button
            onClick={() => openActionModal('RESERVE')}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-amber-500/15 text-amber-400 border border-amber-500/30 hover:bg-amber-500/25 text-xs font-semibold"
          >
            <BookmarkPlus className="w-3.5 h-3.5" />
            <span>Reserve</span>
          </button>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="p-4 bg-slate-900/80 border border-slate-800 rounded-2xl flex flex-col md:flex-row items-center gap-4">
        <div className="relative flex-1 w-full">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search by SKU, product name, or warehouse..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            className="w-full bg-slate-950/80 border border-slate-800 rounded-xl pl-10 pr-4 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-sky-500"
          />
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto">
          <select
            value={warehouseId || ''}
            onChange={(e) => {
              setWarehouseId(e.target.value ? Number(e.target.value) : undefined);
              setPage(0);
            }}
            className="bg-slate-950/80 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-sky-500"
          >
            <option value="">All Warehouses</option>
            {warehouses?.map((w) => (
              <option key={w.id} value={w.id}>
                {w.name} ({w.code})
              </option>
            ))}
          </select>

          <select
            value={stockStatus}
            onChange={(e) => {
              setStockStatus(e.target.value);
              setPage(0);
            }}
            className="bg-slate-950/80 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-sky-500"
          >
            <option value="">All Stock Levels</option>
            <option value="HEALTHY">Healthy Stock</option>
            <option value="LOW_STOCK">Low Stock Alert</option>
            <option value="OUT_OF_STOCK">Out of Stock (0 Available)</option>
          </select>
        </div>
      </div>

      {/* Inventory Matrix Table */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-900/50 text-slate-400 font-medium">
                <th className="p-4">Product / SKU</th>
                <th className="p-4">Warehouse</th>
                <th className="p-4 text-center">Available (Usable)</th>
                <th className="p-4 text-center">Reserved (Orders)</th>
                <th className="p-4 text-center">Damaged (Quarantine)</th>
                <th className="p-4 text-center">Total On-Hand</th>
                <th className="p-4 text-center">Risk Horizon</th>
                <th className="p-4 text-right">Row Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-slate-400">
                    Loading inventory records...
                  </td>
                </tr>
              ) : inventoryData?.content.length === 0 ? (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-slate-400">
                    No inventory records match criteria.
                  </td>
                </tr>
              ) : (
                inventoryData?.content.map((inv) => {
                  const isOut = inv.availableQuantity === 0;
                  const isLow = inv.availableQuantity > 0 && inv.availableQuantity <= inv.product.reorderLevel;

                  return (
                    <tr key={inv.id} className="hover:bg-slate-800/30 transition-colors">
                      <td className="p-4">
                        <div className="font-semibold text-white">{inv.product.name}</div>
                        <div className="text-[11px] font-mono text-sky-400">{inv.product.sku}</div>
                      </td>
                      <td className="p-4">
                        <div className="text-slate-300 font-medium">{inv.warehouse.name}</div>
                        <div className="text-slate-500 text-[10px] font-mono">{inv.warehouse.code}</div>
                      </td>
                      <td className="p-4 text-center font-bold text-sky-400 text-sm">{inv.availableQuantity}</td>
                      <td className="p-4 text-center font-medium text-amber-400">{inv.reservedQuantity}</td>
                      <td className="p-4 text-center font-medium text-rose-400">{inv.damagedQuantity}</td>
                      <td className="p-4 text-center font-bold text-white bg-slate-800/30 text-sm">
                        {inv.onHandQuantity}
                      </td>
                      <td className="p-4 text-center">
                        <Badge
                          variant={
                            inv.calculatedStockoutRisk === 'CRITICAL' || isOut
                              ? 'danger'
                              : inv.calculatedStockoutRisk === 'HIGH' || isLow
                              ? 'warning'
                              : 'success'
                          }
                        >
                          {inv.calculatedStockoutRisk || (isOut ? 'CRITICAL' : isLow ? 'HIGH' : 'LOW')}
                        </Badge>
                      </td>
                      <td className="p-4 text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            onClick={() => openActionModal('IN', inv)}
                            title="Quick Stock In"
                            className="p-1 rounded text-emerald-400 hover:bg-emerald-500/20"
                          >
                            <Plus className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => openActionModal('OUT', inv)}
                            title="Quick Stock Out"
                            className="p-1 rounded text-rose-400 hover:bg-rose-500/20"
                          >
                            <Minus className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => openActionModal('ADJUST', inv)}
                            title="Physical Count Adjustment"
                            className="p-1 rounded text-slate-400 hover:text-white hover:bg-slate-800"
                          >
                            <Sliders className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => openActionModal('DAMAGE', inv)}
                            title="Mark Damaged"
                            className="p-1 rounded text-rose-400 hover:bg-rose-500/20"
                          >
                            <AlertOctagon className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => openActionModal('TRANSFER', inv)}
                            title="Transfer Stock"
                            className="p-1 rounded text-sky-400 hover:bg-sky-500/20"
                          >
                            <ArrowLeftRight className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {inventoryData && inventoryData.totalPages > 1 && (
          <div className="p-4 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
            <span>
              Showing {inventoryData.pageNumber * inventoryData.pageSize + 1} -{' '}
              {Math.min((inventoryData.pageNumber + 1) * inventoryData.pageSize, inventoryData.totalElements)} of{' '}
              {inventoryData.totalElements} records
            </span>
            <div className="flex items-center gap-2">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
                className="px-3 py-1 rounded-lg bg-slate-800 text-white disabled:opacity-40"
              >
                Previous
              </button>
              <span>
                Page {page + 1} of {inventoryData.totalPages}
              </span>
              <button
                disabled={inventoryData.last}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1 rounded-lg bg-slate-800 text-white disabled:opacity-40"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Dynamic Action Modal */}
      <Modal
        isOpen={!!activeAction}
        onClose={() => setActiveAction(null)}
        title={
          activeAction === 'IN'
            ? 'Stock IN Mutation (Receive Inventory)'
            : activeAction === 'OUT'
            ? 'Stock OUT Mutation (Fulfill Order)'
            : activeAction === 'ADJUST'
            ? 'Stock Adjustment (Physical Cycle Count)'
            : activeAction === 'RESERVE'
            ? 'Stock Allocation Reservation'
            : activeAction === 'DAMAGE'
            ? 'Quarantine Damaged Inventory'
            : activeAction === 'RETURN'
            ? 'Process Customer / Internal Return'
            : 'Inter-Warehouse Atomic Transfer'
        }
        subtitle="Executed inside database @Transactional boundary with optimistic locking version check."
        maxWidth="lg"
      >
        <form onSubmit={handleActionSubmit} className="space-y-4 text-xs">
          {errorMessage && (
            <div className="p-3 bg-rose-500/10 border border-rose-500/30 rounded-xl text-rose-400 flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 shrink-0" />
              <span>{errorMessage}</span>
            </div>
          )}

          {/* Product Select */}
          <div>
            <label className="block text-slate-400 mb-1">Product SKU *</label>
            <select
              disabled={!!selectedInventory}
              value={actionForm.productId}
              onChange={(e) => setActionForm({ ...actionForm, productId: Number(e.target.value) })}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white disabled:opacity-60"
            >
              {products?.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} ({p.sku})
                </option>
              ))}
            </select>
          </div>

          {/* Warehouse Selection */}
          {activeAction !== 'TRANSFER' ? (
            <div>
              <label className="block text-slate-400 mb-1">Warehouse Hub *</label>
              <select
                disabled={!!selectedInventory}
                value={actionForm.warehouseId}
                onChange={(e) => setActionForm({ ...actionForm, warehouseId: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white disabled:opacity-60"
              >
                {warehouses?.map((w) => (
                  <option key={w.id} value={w.id}>
                    {w.name} ({w.code})
                  </option>
                ))}
              </select>
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-slate-400 mb-1">Source Warehouse (OUT) *</label>
                <select
                  value={actionForm.sourceWarehouseId}
                  onChange={(e) => setActionForm({ ...actionForm, sourceWarehouseId: Number(e.target.value) })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white"
                >
                  {warehouses?.map((w) => (
                    <option key={w.id} value={w.id}>
                      {w.name} ({w.code})
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-slate-400 mb-1">Destination Warehouse (IN) *</label>
                <select
                  value={actionForm.destinationWarehouseId}
                  onChange={(e) => setActionForm({ ...actionForm, destinationWarehouseId: Number(e.target.value) })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white"
                >
                  {warehouses
                    ?.filter((w) => w.id !== actionForm.sourceWarehouseId)
                    .map((w) => (
                      <option key={w.id} value={w.id}>
                        {w.name} ({w.code})
                      </option>
                    ))}
                </select>
              </div>
            </div>
          )}

          {/* Quantity Input */}
          {activeAction !== 'ADJUST' ? (
            <div>
              <label className="block text-slate-400 mb-1">Quantity *</label>
              <input
                type="number"
                min="1"
                required
                value={actionForm.quantity}
                onChange={(e) => setActionForm({ ...actionForm, quantity: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono"
              />
            </div>
          ) : (
            <div>
              <label className="block text-slate-400 mb-1">Exact Counted Quantity *</label>
              <input
                type="number"
                min="0"
                required
                value={actionForm.resultingQuantity}
                onChange={(e) => setActionForm({ ...actionForm, resultingQuantity: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono"
              />
            </div>
          )}

          {/* Reference Number & Reason */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-slate-400 mb-1">Reference Number / Order ID</label>
              <input
                type="text"
                value={actionForm.referenceNumber}
                onChange={(e) => setActionForm({ ...actionForm, referenceNumber: e.target.value })}
                placeholder="e.g. PO-8819 or ORD-990"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono"
              />
            </div>
            <div>
              <label className="block text-slate-400 mb-1">Reason / Description</label>
              <input
                type="text"
                value={actionForm.reason}
                onChange={(e) => setActionForm({ ...actionForm, reason: e.target.value })}
                placeholder="e.g. Supplier Shipment Receipt"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white"
              />
            </div>
          </div>

          <div className="pt-4 border-t border-slate-800 flex justify-end gap-3">
            <button
              type="button"
              onClick={() => setActiveAction(null)}
              className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 hover:bg-slate-700"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 rounded-xl bg-sky-500 hover:bg-sky-400 text-white font-semibold shadow-lg shadow-sky-500/20"
            >
              Commit Transaction
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
