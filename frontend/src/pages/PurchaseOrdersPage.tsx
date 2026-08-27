import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ShoppingCart,
  Plus,
  Search,
  CheckCircle,
  Send,
  PackageCheck,
  Ban,
  Eye,
  Trash2,
  AlertTriangle,
  Building2,
  Calendar,
  DollarSign,
} from 'lucide-react';
import { purchaseOrderApi, supplierApi, warehouseApi, productApi } from '../services/api';
import { PurchaseOrder, PurchaseOrderStatus, Supplier, Warehouse, Product } from '../types';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';

export const PurchaseOrdersPage: React.FC = () => {
  const queryClient = useQueryClient();

  // Filters
  const [search, setSearch] = useState('');
  const [supplierId, setSupplierId] = useState<number | undefined>();
  const [warehouseId, setWarehouseId] = useState<number | undefined>();
  const [status, setStatus] = useState<string>('');
  const [page, setPage] = useState(0);

  // Modals
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isReceiveModalOpen, setIsReceiveModalOpen] = useState(false);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [selectedPO, setSelectedPO] = useState<PurchaseOrder | null>(null);

  // Receive Form State: Map of itemId -> deltaQty
  const [receivedDeltas, setReceivedDeltas] = useState<Record<number, number>>({});
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Create PO Form State
  const [poForm, setPoForm] = useState({
    supplierId: 1,
    warehouseId: 1,
    expectedDeliveryDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    notes: '',
    items: [{ productId: 1, orderedQuantity: 20, unitCost: 100 }],
  });

  // Queries
  const { data: poData, isLoading } = useQuery({
    queryKey: ['purchase-orders', search, supplierId, warehouseId, status, page],
    queryFn: () =>
      purchaseOrderApi.getPaged({
        search: search || undefined,
        supplierId,
        warehouseId,
        status: status || undefined,
        page,
        size: 10,
      }),
  });

  const { data: suppliers } = useQuery({ queryKey: ['suppliers-all'], queryFn: supplierApi.getAll });
  const { data: warehouses } = useQuery({ queryKey: ['warehouses-all'], queryFn: warehouseApi.getAll });
  const { data: products } = useQuery({ queryKey: ['products-all'], queryFn: productApi.getAll });

  // Mutations
  const invalidatePOQueries = () => {
    queryClient.invalidateQueries({ queryKey: ['purchase-orders'] });
    queryClient.invalidateQueries({ queryKey: ['inventories'] });
    queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
    queryClient.invalidateQueries({ queryKey: ['products'] });
    queryClient.invalidateQueries({ queryKey: ['transactions'] });
    setErrorMessage(null);
  };

  const createMutation = useMutation({
    mutationFn: purchaseOrderApi.create,
    onSuccess: () => {
      invalidatePOQueries();
      setIsCreateModalOpen(false);
    },
    onError: (err: any) => setErrorMessage(err.response?.data?.message || err.message),
  });

  const approveMutation = useMutation({
    mutationFn: purchaseOrderApi.approve,
    onSuccess: invalidatePOQueries,
    onError: (err: any) => alert(err.response?.data?.message || err.message),
  });

  const orderMutation = useMutation({
    mutationFn: purchaseOrderApi.order,
    onSuccess: invalidatePOQueries,
    onError: (err: any) => alert(err.response?.data?.message || err.message),
  });

  const receiveMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: any }) => purchaseOrderApi.receive(id, data),
    onSuccess: () => {
      invalidatePOQueries();
      setIsReceiveModalOpen(false);
    },
    onError: (err: any) => setErrorMessage(err.response?.data?.message || err.message),
  });

  const cancelMutation = useMutation({
    mutationFn: purchaseOrderApi.cancel,
    onSuccess: invalidatePOQueries,
    onError: (err: any) => alert(err.response?.data?.message || err.message),
  });

  const getStatusBadge = (s: PurchaseOrderStatus) => {
    switch (s) {
      case 'DRAFT':
        return <Badge variant="neutral">DRAFT</Badge>;
      case 'PENDING':
        return <Badge variant="warning">PENDING</Badge>;
      case 'APPROVED':
        return <Badge variant="purple">APPROVED</Badge>;
      case 'ORDERED':
        return <Badge variant="info">ORDERED</Badge>;
      case 'PARTIALLY_RECEIVED':
        return <Badge variant="warning">PARTIALLY RECEIVED</Badge>;
      case 'RECEIVED':
        return <Badge variant="success">RECEIVED</Badge>;
      case 'CANCELLED':
        return <Badge variant="danger">CANCELLED</Badge>;
    }
  };

  // Line Item Handlers
  const addLineItem = () => {
    setPoForm({
      ...poForm,
      items: [...poForm.items, { productId: products?.[0]?.id || 1, orderedQuantity: 10, unitCost: 50 }],
    });
  };

  const removeLineItem = (index: number) => {
    if (poForm.items.length <= 1) return;
    const items = [...poForm.items];
    items.splice(index, 1);
    setPoForm({ ...poForm, items });
  };

  const updateLineItem = (index: number, field: string, value: any) => {
    const items = [...poForm.items];
    items[index] = { ...items[index], [field]: value };

    // Auto-update unit cost when product changes
    if (field === 'productId') {
      const prod = products?.find((p) => p.id === Number(value));
      if (prod) {
        items[index].unitCost = prod.costPrice;
      }
    }
    setPoForm({ ...poForm, items });
  };

  const totalCalculatedAmount = poForm.items.reduce(
    (acc, item) => acc + item.orderedQuantity * item.unitCost,
    0
  );

  const openReceiveModal = (po: PurchaseOrder) => {
    setSelectedPO(po);
    const initialDeltas: Record<number, number> = {};
    po.items.forEach((item) => {
      const remaining = item.orderedQuantity - item.receivedQuantity;
      initialDeltas[item.id] = remaining > 0 ? remaining : 0;
    });
    setReceivedDeltas(initialDeltas);
    setErrorMessage(null);
    setIsReceiveModalOpen(true);
  };

  const handleReceiveSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedPO) return;

    const itemsToReceive = Object.entries(receivedDeltas)
      .map(([itemId, delta]) => ({
        itemId: Number(itemId),
        receivedQuantityDelta: Number(delta),
      }))
      .filter((i) => i.receivedQuantityDelta > 0);

    if (itemsToReceive.length === 0) {
      setErrorMessage('Please specify at least one line item quantity to receive.');
      return;
    }

    receiveMutation.mutate({ id: selectedPO.id, data: { items: itemsToReceive } });
  };

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-white tracking-tight flex items-center gap-2.5">
            <ShoppingCart className="w-6 h-6 text-sky-400" />
            <span>Purchase Order Management</span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Supplier procurement workflow: <code>PENDING &rarr; APPROVED &rarr; ORDERED &rarr; RECEIVE</code>
          </p>
        </div>
        <button
          onClick={() => {
            setErrorMessage(null);
            setIsCreateModalOpen(true);
          }}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-sky-500 hover:bg-sky-400 text-white text-xs font-semibold shadow-lg shadow-sky-500/20 transition-all hover:scale-105 shrink-0"
        >
          <Plus className="w-4 h-4" />
          <span>Create Purchase Order</span>
        </button>
      </div>

      {/* Filter Toolbar */}
      <div className="p-4 bg-slate-900/80 border border-slate-800 rounded-2xl flex flex-col md:flex-row items-center gap-4">
        <div className="relative flex-1 w-full">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search by PO Number or Supplier..."
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
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(0);
            }}
            className="bg-slate-950/80 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-sky-500"
          >
            <option value="">All Statuses</option>
            <option value="PENDING">PENDING</option>
            <option value="APPROVED">APPROVED</option>
            <option value="ORDERED">ORDERED</option>
            <option value="PARTIALLY_RECEIVED">PARTIALLY RECEIVED</option>
            <option value="RECEIVED">RECEIVED</option>
            <option value="CANCELLED">CANCELLED</option>
          </select>

          <select
            value={supplierId || ''}
            onChange={(e) => {
              setSupplierId(e.target.value ? Number(e.target.value) : undefined);
              setPage(0);
            }}
            className="bg-slate-950/80 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-sky-500 max-w-[180px]"
          >
            <option value="">All Suppliers</option>
            {suppliers?.map((s) => (
              <option key={s.id} value={s.id}>
                {s.companyName}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* PO Table */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-900/50 text-slate-400 font-medium">
                <th className="p-4">PO Number &amp; Date</th>
                <th className="p-4">Supplier</th>
                <th className="p-4">Destination Hub</th>
                <th className="p-4 text-right">Items &amp; Total Value</th>
                <th className="p-4 text-center">Lifecycle Status</th>
                <th className="p-4 text-right">Workflow Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-slate-400">
                    Loading purchase orders...
                  </td>
                </tr>
              ) : poData?.content.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-slate-400">
                    No purchase orders found matching criteria.
                  </td>
                </tr>
              ) : (
                poData?.content.map((po) => (
                  <tr key={po.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="p-4 font-mono">
                      <div className="font-bold text-white text-sm">{po.poNumber}</div>
                      <div className="text-slate-500 text-[11px]">{po.orderDate}</div>
                    </td>
                    <td className="p-4">
                      <div className="font-semibold text-slate-200">{po.supplier.companyName}</div>
                      <div className="text-[10px] text-slate-400">{po.supplier.email}</div>
                    </td>
                    <td className="p-4 text-slate-300 font-medium">
                      <div>{po.warehouse.name}</div>
                      <div className="text-[10px] text-slate-500 font-mono">{po.warehouse.code}</div>
                    </td>
                    <td className="p-4 text-right font-mono">
                      <div className="text-white font-bold text-sm">${po.totalAmount.toFixed(2)}</div>
                      <div className="text-slate-400 text-[10px]">{po.items?.length || 0} line items</div>
                    </td>
                    <td className="p-4 text-center">{getStatusBadge(po.status)}</td>
                    <td className="p-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        {/* Status Transition Action Buttons */}
                        {po.status === 'PENDING' && (
                          <button
                            onClick={() => approveMutation.mutate(po.id)}
                            title="Approve Purchase Order"
                            className="px-2.5 py-1 rounded-lg bg-purple-500/20 text-purple-300 hover:bg-purple-500/30 border border-purple-500/30 font-semibold text-[11px] flex items-center gap-1"
                          >
                            <CheckCircle className="w-3 h-3" />
                            <span>Approve</span>
                          </button>
                        )}

                        {po.status === 'APPROVED' && (
                          <button
                            onClick={() => orderMutation.mutate(po.id)}
                            title="Transmit Order to Supplier"
                            className="px-2.5 py-1 rounded-lg bg-sky-500/20 text-sky-300 hover:bg-sky-500/30 border border-sky-500/30 font-semibold text-[11px] flex items-center gap-1"
                          >
                            <Send className="w-3 h-3" />
                            <span>Order</span>
                          </button>
                        )}

                        {(po.status === 'ORDERED' || po.status === 'PARTIALLY_RECEIVED') && (
                          <button
                            onClick={() => openReceiveModal(po)}
                            title="Receive Incoming Stock"
                            className="px-2.5 py-1 rounded-lg bg-emerald-500/20 text-emerald-300 hover:bg-emerald-500/30 border border-emerald-500/30 font-semibold text-[11px] flex items-center gap-1"
                          >
                            <PackageCheck className="w-3 h-3" />
                            <span>Receive</span>
                          </button>
                        )}

                        {po.status !== 'RECEIVED' && po.status !== 'CANCELLED' && (
                          <button
                            onClick={() => {
                              if (confirm(`Cancel Purchase Order ${po.poNumber}?`)) {
                                cancelMutation.mutate(po.id);
                              }
                            }}
                            title="Cancel Purchase Order"
                            className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                          >
                            <Ban className="w-4 h-4" />
                          </button>
                        )}

                        <button
                          onClick={() => {
                            setSelectedPO(po);
                            setIsDetailModalOpen(true);
                          }}
                          title="View PO Details"
                          className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {poData && poData.totalPages > 1 && (
          <div className="p-4 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
            <span>
              Showing {poData.pageNumber * poData.pageSize + 1} -{' '}
              {Math.min((poData.pageNumber + 1) * poData.pageSize, poData.totalElements)} of {poData.totalElements} orders
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
                Page {page + 1} of {poData.totalPages}
              </span>
              <button
                disabled={poData.last}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1 rounded-lg bg-slate-800 text-white disabled:opacity-40"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Create PO Modal */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Create New Purchase Order"
        subtitle="Initiate procurement lifecycle in PENDING status."
        maxWidth="3xl"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate(poForm);
          }}
          className="space-y-4 text-xs"
        >
          {errorMessage && (
            <div className="p-3 bg-rose-500/10 border border-rose-500/30 rounded-xl text-rose-400 flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 shrink-0" />
              <span>{errorMessage}</span>
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-slate-400 mb-1">Supplier *</label>
              <select
                value={poForm.supplierId}
                onChange={(e) => setPoForm({ ...poForm, supplierId: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white"
              >
                {suppliers?.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.companyName}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-slate-400 mb-1">Target Warehouse *</label>
              <select
                value={poForm.warehouseId}
                onChange={(e) => setPoForm({ ...poForm, warehouseId: Number(e.target.value) })}
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
              <label className="block text-slate-400 mb-1">Expected Delivery Date</label>
              <input
                type="date"
                value={poForm.expectedDeliveryDate}
                onChange={(e) => setPoForm({ ...poForm, expectedDeliveryDate: e.target.value })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono"
              />
            </div>
          </div>

          <div>
            <label className="block text-slate-400 mb-1">Order Notes</label>
            <input
              type="text"
              placeholder="e.g. Stock replenishment for Q3 enterprise contracts"
              value={poForm.notes}
              onChange={(e) => setPoForm({ ...poForm, notes: e.target.value })}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white"
            />
          </div>

          {/* Line Items Dynamic Form */}
          <div className="pt-2 border-t border-slate-800">
            <div className="flex items-center justify-between mb-3">
              <span className="font-semibold text-white">Purchase Order Line Items</span>
              <button
                type="button"
                onClick={addLineItem}
                className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-800 text-sky-400 hover:bg-slate-700 font-semibold text-[11px]"
              >
                <Plus className="w-3 h-3" />
                <span>Add Item</span>
              </button>
            </div>

            <div className="space-y-3">
              {poForm.items.map((item, idx) => (
                <div key={idx} className="p-3 bg-slate-950/80 border border-slate-800 rounded-xl grid grid-cols-12 gap-3 items-center">
                  <div className="col-span-5">
                    <label className="block text-[10px] text-slate-500 mb-0.5">Product SKU</label>
                    <select
                      value={item.productId}
                      onChange={(e) => updateLineItem(idx, 'productId', Number(e.target.value))}
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg px-2 py-1.5 text-white"
                    >
                      {products?.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.name} ({p.sku})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="col-span-3">
                    <label className="block text-[10px] text-slate-500 mb-0.5">Quantity</label>
                    <input
                      type="number"
                      min="1"
                      required
                      value={item.orderedQuantity}
                      onChange={(e) => updateLineItem(idx, 'orderedQuantity', Number(e.target.value))}
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg px-2 py-1.5 text-white font-mono"
                    />
                  </div>

                  <div className="col-span-3">
                    <label className="block text-[10px] text-slate-500 mb-0.5">Unit Cost ($)</label>
                    <input
                      type="number"
                      step="0.01"
                      min="0.01"
                      required
                      value={item.unitCost}
                      onChange={(e) => updateLineItem(idx, 'unitCost', Number(e.target.value))}
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg px-2 py-1.5 text-white font-mono"
                    />
                  </div>

                  <div className="col-span-1 flex justify-center pt-3">
                    <button
                      type="button"
                      onClick={() => removeLineItem(idx)}
                      disabled={poForm.items.length <= 1}
                      className="p-1 rounded text-slate-500 hover:text-rose-400 disabled:opacity-30"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-4 p-3 bg-slate-950 rounded-xl flex items-center justify-between border border-slate-800">
              <span className="font-semibold text-slate-300">Total Order Valuation:</span>
              <span className="font-bold text-sky-400 text-base font-mono">${totalCalculatedAmount.toFixed(2)}</span>
            </div>
          </div>

          <div className="pt-4 border-t border-slate-800 flex justify-end gap-3">
            <button
              type="button"
              onClick={() => setIsCreateModalOpen(false)}
              className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 hover:bg-slate-700"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="px-4 py-2 rounded-xl bg-sky-500 hover:bg-sky-400 text-white font-semibold shadow-lg shadow-sky-500/20"
            >
              {createMutation.isPending ? 'Creating...' : 'Submit Purchase Order'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Receive PO Line Items Modal */}
      {selectedPO && (
        <Modal
          isOpen={isReceiveModalOpen}
          onClose={() => setIsReceiveModalOpen(false)}
          title={`Receive Stock: ${selectedPO.poNumber}`}
          subtitle={`Supplier: ${selectedPO.supplier.companyName} • Destination: ${selectedPO.warehouse.name}`}
          maxWidth="2xl"
        >
          <form onSubmit={handleReceiveSubmit} className="space-y-4 text-xs">
            {errorMessage && (
              <div className="p-3 bg-rose-500/10 border border-rose-500/30 rounded-xl text-rose-400 flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 shrink-0" />
                <span>{errorMessage}</span>
              </div>
            )}

            <p className="text-slate-400">
              Specify the number of physical units delivered per line item. Receiving increments available inventory and records an atomic{' '}
              <code>STOCK_IN</code> transaction ledger entry.
            </p>

            <div className="space-y-3">
              {selectedPO.items.map((item) => {
                const remaining = item.orderedQuantity - item.receivedQuantity;
                return (
                  <div key={item.id} className="p-3.5 bg-slate-950/80 border border-slate-800 rounded-xl space-y-2">
                    <div className="flex items-center justify-between">
                      <div>
                        <span className="font-semibold text-white">{item.product.name}</span>
                        <span className="text-[11px] font-mono text-sky-400 ml-2">{item.product.sku}</span>
                      </div>
                      <span className="font-mono text-slate-400">
                        Ordered: <strong>{item.orderedQuantity}</strong> | Received: <strong>{item.receivedQuantity}</strong>
                      </span>
                    </div>

                    <div className="flex items-center justify-between pt-2 border-t border-slate-800">
                      <span className="text-amber-400 font-medium">Pending Delivery: {remaining} units</span>
                      <div className="flex items-center gap-2">
                        <label className="text-slate-400">Receiving Now:</label>
                        <input
                          type="number"
                          min="0"
                          max={remaining}
                          value={receivedDeltas[item.id] || 0}
                          onChange={(e) =>
                            setReceivedDeltas({ ...receivedDeltas, [item.id]: Math.min(remaining, Number(e.target.value)) })
                          }
                          className="w-20 bg-slate-900 border border-slate-700 rounded-lg px-2 py-1 text-white font-mono text-center font-bold"
                        />
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="pt-4 border-t border-slate-800 flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setIsReceiveModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 hover:bg-slate-700"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={receiveMutation.isPending}
                className="px-4 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-white font-semibold shadow-lg shadow-emerald-500/20 flex items-center gap-1.5"
              >
                <PackageCheck className="w-4 h-4" />
                <span>{receiveMutation.isPending ? 'Processing...' : 'Confirm Delivery Receipt'}</span>
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* PO Detail View Modal */}
      {selectedPO && (
        <Modal
          isOpen={isDetailModalOpen}
          onClose={() => setIsDetailModalOpen(false)}
          title={`Purchase Order: ${selectedPO.poNumber}`}
          subtitle={`Status: ${selectedPO.status} • Order Date: ${selectedPO.orderDate}`}
          maxWidth="2xl"
        >
          <div className="space-y-4 text-xs">
            <div className="grid grid-cols-2 gap-3 p-3.5 bg-slate-950 rounded-xl border border-slate-800">
              <div>
                <span className="text-slate-400">Supplier:</span>
                <p className="font-semibold text-white mt-0.5">{selectedPO.supplier.companyName}</p>
                <p className="text-slate-500 text-[10px]">{selectedPO.supplier.contactPerson}</p>
              </div>
              <div>
                <span className="text-slate-400">Destination Hub:</span>
                <p className="font-semibold text-white mt-0.5">{selectedPO.warehouse.name}</p>
                <p className="text-slate-500 text-[10px] font-mono">{selectedPO.warehouse.code}</p>
              </div>
            </div>

            <div className="p-3.5 bg-slate-950 rounded-xl border border-slate-800">
              <h4 className="font-semibold text-white mb-2">Order Line Items</h4>
              <div className="divide-y divide-slate-800">
                {selectedPO.items.map((item) => (
                  <div key={item.id} className="py-2 flex items-center justify-between">
                    <div>
                      <span className="font-semibold text-white">{item.product.name}</span>
                      <span className="font-mono text-sky-400 text-[11px] ml-2">({item.product.sku})</span>
                    </div>
                    <div className="text-right font-mono">
                      <div>
                        {item.receivedQuantity} / {item.orderedQuantity} units @ ${item.unitCost.toFixed(2)}
                      </div>
                      <div className="font-bold text-white">${item.totalCost.toFixed(2)}</div>
                    </div>
                  </div>
                ))}
              </div>
              <div className="mt-3 pt-3 border-t border-slate-800 flex justify-between font-bold text-white text-sm">
                <span>Total Amount:</span>
                <span className="font-mono text-sky-400">${selectedPO.totalAmount.toFixed(2)}</span>
              </div>
            </div>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setIsDetailModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-slate-800 text-white hover:bg-slate-700"
              >
                Close
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
