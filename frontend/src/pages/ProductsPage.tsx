import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Boxes,
  Plus,
  Search,
  Filter,
  Eye,
  Edit2,
  Trash2,
  TrendingUp,
  Bot,
  AlertCircle,
  CheckCircle2,
  DollarSign,
  Package,
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { productApi, categoryApi, supplierApi, inventoryApi, analyticsApi, aiApi } from '../services/api';
import { Product, Category, Supplier, Inventory, DemandForecast, StockoutRisk, ReorderRecommendation, AIChatResponse } from '../types';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';

export const ProductsPage: React.FC = () => {
  const queryClient = useQueryClient();

  // Search & Filter State
  const [search, setSearch] = useState('');
  const [categoryId, setCategoryId] = useState<number | undefined>();
  const [supplierId, setSupplierId] = useState<number | undefined>();
  const [page, setPage] = useState(0);

  // Modal States
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);

  // Form State
  const [formData, setFormData] = useState({
    sku: '',
    name: '',
    description: '',
    categoryId: 1,
    supplierId: 1,
    costPrice: 100,
    sellingPrice: 150,
    unit: 'unit',
    reorderLevel: 10,
    reorderQuantity: 20,
    active: true,
  });

  // Queries
  const { data: productsData, isLoading } = useQuery({
    queryKey: ['products', search, categoryId, supplierId, page],
    queryFn: () => productApi.getPaged({ search: search || undefined, categoryId, supplierId, page, size: 10 }),
  });

  const { data: categories } = useQuery({
    queryKey: ['categories-all'],
    queryFn: categoryApi.getAll,
  });

  const { data: suppliers } = useQuery({
    queryKey: ['suppliers-all'],
    queryFn: supplierApi.getAll,
  });

  // Detail Modal Queries
  const { data: productInventories } = useQuery({
    queryKey: ['product-inventories', selectedProduct?.id],
    queryFn: () => (selectedProduct ? inventoryApi.getByProduct(selectedProduct.id) : Promise.resolve([])),
    enabled: !!selectedProduct && isDetailModalOpen,
  });

  const { data: productForecast } = useQuery({
    queryKey: ['product-forecast', selectedProduct?.id],
    queryFn: () => (selectedProduct ? analyticsApi.getForecast(selectedProduct.id, 'MOVING_AVERAGE', '14_DAYS') : Promise.resolve(null)),
    enabled: !!selectedProduct && isDetailModalOpen,
  });

  const { data: productRisk } = useQuery({
    queryKey: ['product-risk', selectedProduct?.id],
    queryFn: () => (selectedProduct ? analyticsApi.getProductStockoutRisk(selectedProduct.id) : Promise.resolve(null)),
    enabled: !!selectedProduct && isDetailModalOpen,
  });

  const { data: productReorder } = useQuery({
    queryKey: ['product-reorder', selectedProduct?.id],
    queryFn: () => (selectedProduct ? analyticsApi.getProductReorder(selectedProduct.id) : Promise.resolve(null)),
    enabled: !!selectedProduct && isDetailModalOpen,
  });

  // AI Explanation State
  const [aiExplanation, setAiExplanation] = useState<string | null>(null);
  const [isAiLoading, setIsAiLoading] = useState(false);

  // Mutations
  const createMutation = useMutation({
    mutationFn: productApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      setIsCreateModalOpen(false);
      resetForm();
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: any }) => productApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      setIsEditModalOpen(false);
      resetForm();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: productApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
    },
  });

  const resetForm = () => {
    setFormData({
      sku: '',
      name: '',
      description: '',
      categoryId: categories?.[0]?.id || 1,
      supplierId: suppliers?.[0]?.id || 1,
      costPrice: 100,
      sellingPrice: 150,
      unit: 'unit',
      reorderLevel: 10,
      reorderQuantity: 20,
      active: true,
    });
    setSelectedProduct(null);
  };

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    createMutation.mutate(formData);
  };

  const handleEditSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedProduct) {
      updateMutation.mutate({ id: selectedProduct.id, data: formData });
    }
  };

  const openEditModal = (p: Product) => {
    setSelectedProduct(p);
    setFormData({
      sku: p.sku,
      name: p.name,
      description: p.description || '',
      categoryId: p.category.id,
      supplierId: p.supplier.id,
      costPrice: p.costPrice,
      sellingPrice: p.sellingPrice,
      unit: p.unit,
      reorderLevel: p.reorderLevel,
      reorderQuantity: p.reorderQuantity,
      active: p.active,
    });
    setIsEditModalOpen(true);
  };

  const openDetailModal = (p: Product) => {
    setSelectedProduct(p);
    setAiExplanation(null);
    setIsDetailModalOpen(true);
  };

  const triggerAiExplanation = async () => {
    if (!selectedProduct) return;
    setIsAiLoading(true);
    try {
      const response = await aiApi.explainProduct(selectedProduct.id);
      setAiExplanation(response.reply);
    } catch (err) {
      setAiExplanation('Unable to contact AI Copilot. Statistical intelligence values above remain authoritative.');
    } finally {
      setIsAiLoading(false);
    }
  };

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-white tracking-tight flex items-center gap-2.5">
            <Boxes className="w-6 h-6 text-sky-400" />
            <span>Product Catalog</span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Authoritative master SKU records, unit costs, and inventory allocation across hubs.
          </p>
        </div>
        <button
          onClick={() => {
            resetForm();
            setIsCreateModalOpen(true);
          }}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-sky-500 hover:bg-sky-400 text-white text-xs font-semibold shadow-lg shadow-sky-500/20 transition-all hover:scale-105 shrink-0"
        >
          <Plus className="w-4 h-4" />
          <span>New Product</span>
        </button>
      </div>

      {/* Filter Toolbar */}
      <div className="p-4 bg-slate-900/80 border border-slate-800 rounded-2xl flex flex-col md:flex-row items-center gap-4">
        <div className="relative flex-1 w-full">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search by SKU or product name..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            className="w-full bg-slate-950/80 border border-slate-800 rounded-xl pl-10 pr-4 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-sky-500 transition-colors"
          />
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto">
          <select
            value={categoryId || ''}
            onChange={(e) => {
              setCategoryId(e.target.value ? Number(e.target.value) : undefined);
              setPage(0);
            }}
            className="bg-slate-950/80 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-sky-500"
          >
            <option value="">All Categories</option>
            {categories?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>

          <select
            value={supplierId || ''}
            onChange={(e) => {
              setSupplierId(e.target.value ? Number(e.target.value) : undefined);
              setPage(0);
            }}
            className="bg-slate-950/80 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-sky-500"
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

      {/* Product Table */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-900/50 text-slate-400 font-medium">
                <th className="p-4">SKU &amp; Product</th>
                <th className="p-4">Category</th>
                <th className="p-4">Supplier</th>
                <th className="p-4 text-right">Cost / Price</th>
                <th className="p-4 text-center">Available</th>
                <th className="p-4 text-center">Reserved</th>
                <th className="p-4 text-center">Damaged</th>
                <th className="p-4 text-center">On-Hand</th>
                <th className="p-4 text-center">Status</th>
                <th className="p-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {isLoading ? (
                <tr>
                  <td colSpan={10} className="p-8 text-center text-slate-400">
                    Loading products...
                  </td>
                </tr>
              ) : productsData?.content.length === 0 ? (
                <tr>
                  <td colSpan={10} className="p-8 text-center text-slate-400">
                    No products found matching filters.
                  </td>
                </tr>
              ) : (
                productsData?.content.map((p) => {
                  const isLow = p.totalAvailableQuantity > 0 && p.totalAvailableQuantity <= p.reorderLevel;
                  const isOut = p.totalAvailableQuantity === 0;

                  return (
                    <tr key={p.id} className="hover:bg-slate-800/30 transition-colors">
                      <td className="p-4">
                        <div className="font-semibold text-white">{p.name}</div>
                        <div className="text-[11px] font-mono text-sky-400">{p.sku}</div>
                      </td>
                      <td className="p-4 text-slate-300">{p.category?.name}</td>
                      <td className="p-4 text-slate-400 truncate max-w-[140px]">{p.supplier?.companyName}</td>
                      <td className="p-4 text-right font-mono">
                        <div className="text-white">${p.costPrice.toFixed(2)}</div>
                        <div className="text-slate-500 text-[10px]">${p.sellingPrice.toFixed(2)} sell</div>
                      </td>
                      <td className="p-4 text-center font-bold text-sky-400">{p.totalAvailableQuantity}</td>
                      <td className="p-4 text-center font-medium text-amber-400">{p.totalReservedQuantity}</td>
                      <td className="p-4 text-center font-medium text-rose-400">{p.totalDamagedQuantity}</td>
                      <td className="p-4 text-center font-bold text-white bg-slate-800/20">{p.totalOnHandQuantity}</td>
                      <td className="p-4 text-center">
                        {isOut ? (
                          <Badge variant="danger">Out of Stock</Badge>
                        ) : isLow ? (
                          <Badge variant="warning">Low Stock</Badge>
                        ) : (
                          <Badge variant="success">Healthy</Badge>
                        )}
                      </td>
                      <td className="p-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            onClick={() => openDetailModal(p)}
                            title="View Intelligence & Warehouse Breakdown"
                            className="p-1.5 rounded-lg text-slate-400 hover:text-sky-400 hover:bg-sky-500/10 transition-colors"
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => openEditModal(p)}
                            title="Edit Product"
                            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => {
                              if (confirm(`Deactivate/Delete product '${p.name}'?`)) {
                                deleteMutation.mutate(p.id);
                              }
                            }}
                            title="Deactivate Product"
                            className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                          >
                            <Trash2 className="w-4 h-4" />
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
        {productsData && productsData.totalPages > 1 && (
          <div className="p-4 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
            <span>
              Showing {productsData.pageNumber * productsData.pageSize + 1} -{' '}
              {Math.min((productsData.pageNumber + 1) * productsData.pageSize, productsData.totalElements)} of{' '}
              {productsData.totalElements} products
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
                Page {page + 1} of {productsData.totalPages}
              </span>
              <button
                disabled={productsData.last}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1 rounded-lg bg-slate-800 text-white disabled:opacity-40"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Create Product Modal */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Add New Catalog Product"
        subtitle="Registers SKU for inventory tracking and automatic demand intelligence."
        maxWidth="2xl"
      >
        <form onSubmit={handleCreateSubmit} className="space-y-4 text-xs">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-slate-400 mb-1">SKU Code *</label>
              <input
                type="text"
                required
                value={formData.sku}
                onChange={(e) => setFormData({ ...formData, sku: e.target.value })}
                placeholder="e.g. PROD-LAP-005"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono"
              />
            </div>
            <div>
              <label className="block text-slate-400 mb-1">Product Name *</label>
              <input
                type="text"
                required
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="e.g. Enterprise Laptop Pro"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white"
              />
            </div>
          </div>

          <div>
            <label className="block text-slate-400 mb-1">Description</label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              rows={2}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-slate-400 mb-1">Category *</label>
              <select
                value={formData.categoryId}
                onChange={(e) => setFormData({ ...formData, categoryId: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white"
              >
                {categories?.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-slate-400 mb-1">Supplier *</label>
              <select
                value={formData.supplierId}
                onChange={(e) => setFormData({ ...formData, supplierId: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white"
              >
                {suppliers?.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.companyName}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div>
              <label className="block text-slate-400 mb-1">Cost Price ($)</label>
              <input
                type="number"
                step="0.01"
                required
                value={formData.costPrice}
                onChange={(e) => setFormData({ ...formData, costPrice: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono"
              />
            </div>
            <div>
              <label className="block text-slate-400 mb-1">Selling Price ($)</label>
              <input
                type="number"
                step="0.01"
                required
                value={formData.sellingPrice}
                onChange={(e) => setFormData({ ...formData, sellingPrice: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono"
              />
            </div>
            <div>
              <label className="block text-slate-400 mb-1">Reorder Level</label>
              <input
                type="number"
                required
                value={formData.reorderLevel}
                onChange={(e) => setFormData({ ...formData, reorderLevel: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white"
              />
            </div>
            <div>
              <label className="block text-slate-400 mb-1">Reorder Qty</label>
              <input
                type="number"
                required
                value={formData.reorderQuantity}
                onChange={(e) => setFormData({ ...formData, reorderQuantity: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white"
              />
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
              {createMutation.isPending ? 'Creating...' : 'Save Product'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Edit Product Modal */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        title={`Edit Product: ${formData.sku}`}
        subtitle="Modify product attributes and reorder triggers."
        maxWidth="2xl"
      >
        <form onSubmit={handleEditSubmit} className="space-y-4 text-xs">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-slate-400 mb-1">Product Name *</label>
              <input
                type="text"
                required
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white"
              />
            </div>
            <div>
              <label className="block text-slate-400 mb-1">Unit of Measure</label>
              <input
                type="text"
                required
                value={formData.unit}
                onChange={(e) => setFormData({ ...formData, unit: e.target.value })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white"
              />
            </div>
          </div>

          <div>
            <label className="block text-slate-400 mb-1">Description</label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              rows={2}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-slate-400 mb-1">Category *</label>
              <select
                value={formData.categoryId}
                onChange={(e) => setFormData({ ...formData, categoryId: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white"
              >
                {categories?.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-slate-400 mb-1">Supplier *</label>
              <select
                value={formData.supplierId}
                onChange={(e) => setFormData({ ...formData, supplierId: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white"
              >
                {suppliers?.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.companyName}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div>
              <label className="block text-slate-400 mb-1">Cost Price ($)</label>
              <input
                type="number"
                step="0.01"
                required
                value={formData.costPrice}
                onChange={(e) => setFormData({ ...formData, costPrice: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono"
              />
            </div>
            <div>
              <label className="block text-slate-400 mb-1">Selling Price ($)</label>
              <input
                type="number"
                step="0.01"
                required
                value={formData.sellingPrice}
                onChange={(e) => setFormData({ ...formData, sellingPrice: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white font-mono"
              />
            </div>
            <div>
              <label className="block text-slate-400 mb-1">Reorder Level</label>
              <input
                type="number"
                required
                value={formData.reorderLevel}
                onChange={(e) => setFormData({ ...formData, reorderLevel: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white"
              />
            </div>
            <div>
              <label className="block text-slate-400 mb-1">Reorder Qty</label>
              <input
                type="number"
                required
                value={formData.reorderQuantity}
                onChange={(e) => setFormData({ ...formData, reorderQuantity: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white"
              />
            </div>
          </div>

          <div className="flex items-center gap-2 pt-2">
            <input
              type="checkbox"
              id="activeCheckbox"
              checked={formData.active}
              onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
              className="rounded bg-slate-950 border-slate-800 text-sky-500"
            />
            <label htmlFor="activeCheckbox" className="text-slate-300">
              Active in Product Catalog
            </label>
          </div>

          <div className="pt-4 border-t border-slate-800 flex justify-end gap-3">
            <button
              type="button"
              onClick={() => setIsEditModalOpen(false)}
              className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 hover:bg-slate-700"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={updateMutation.isPending}
              className="px-4 py-2 rounded-xl bg-sky-500 hover:bg-sky-400 text-white font-semibold shadow-lg shadow-sky-500/20"
            >
              {updateMutation.isPending ? 'Saving...' : 'Update Product'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Product Details & Intelligence Modal */}
      {selectedProduct && (
        <Modal
          isOpen={isDetailModalOpen}
          onClose={() => setIsDetailModalOpen(false)}
          title={`${selectedProduct.name}`}
          subtitle={`SKU: ${selectedProduct.sku} • Category: ${selectedProduct.category?.name}`}
          maxWidth="3xl"
        >
          <div className="space-y-6 text-xs">
            {/* Quick Metrics Bar */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div className="p-3 bg-slate-950/80 border border-slate-800 rounded-xl">
                <span className="text-slate-400">Total Available</span>
                <p className="text-lg font-bold text-sky-400 mt-1">{selectedProduct.totalAvailableQuantity} units</p>
              </div>
              <div className="p-3 bg-slate-950/80 border border-slate-800 rounded-xl">
                <span className="text-slate-400">Committed (Reserved)</span>
                <p className="text-lg font-bold text-amber-400 mt-1">{selectedProduct.totalReservedQuantity} units</p>
              </div>
              <div className="p-3 bg-slate-950/80 border border-slate-800 rounded-xl">
                <span className="text-slate-400">Quarantined (Damaged)</span>
                <p className="text-lg font-bold text-rose-400 mt-1">{selectedProduct.totalDamagedQuantity} units</p>
              </div>
              <div className="p-3 bg-slate-950/80 border border-slate-800 rounded-xl">
                <span className="text-slate-400">Physical On-Hand</span>
                <p className="text-lg font-bold text-white mt-1">{selectedProduct.totalOnHandQuantity} units</p>
              </div>
            </div>

            {/* Warehouse Allocation Breakdown */}
            <div className="bg-slate-950/60 border border-slate-800/80 rounded-xl p-4">
              <h4 className="text-sm font-semibold text-white mb-3">Warehouse Stock Distribution</h4>
              <div className="space-y-2">
                {productInventories?.map((inv) => (
                  <div key={inv.id} className="flex items-center justify-between p-2.5 bg-slate-900/60 rounded-lg border border-slate-800/50">
                    <div>
                      <span className="font-semibold text-white">{inv.warehouse.name}</span>
                      <span className="text-slate-400 text-[11px] ml-2 font-mono">({inv.warehouse.code})</span>
                    </div>
                    <div className="flex items-center gap-4">
                      <span className="text-sky-400">Avail: <strong>{inv.availableQuantity}</strong></span>
                      <span className="text-amber-400">Res: <strong>{inv.reservedQuantity}</strong></span>
                      <span className="text-rose-400">Dam: <strong>{inv.damagedQuantity}</strong></span>
                      <span className="text-white font-bold bg-slate-800 px-2 py-0.5 rounded">Total: {inv.onHandQuantity}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Statistical Intelligence (Risk & Forecast) */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Stockout Risk Card */}
              <div className="p-4 bg-slate-950/60 border border-slate-800/80 rounded-xl space-y-2">
                <div className="flex items-center justify-between">
                  <h4 className="text-sm font-semibold text-white">Stockout Risk Diagnosis</h4>
                  {productRisk && (
                    <Badge variant={productRisk.riskLevel === 'CRITICAL' ? 'danger' : productRisk.riskLevel === 'HIGH' ? 'warning' : 'success'}>
                      {productRisk.riskLevel}
                    </Badge>
                  )}
                </div>
                {productRisk ? (
                  <>
                    <p className="text-slate-300 mt-1">{productRisk.explanation}</p>
                    <div className="pt-2 border-t border-slate-800 flex justify-between text-[11px] text-slate-400">
                      <span>Daily Run Rate: <strong>{productRisk.averageDailyDemand} units/day</strong></span>
                      <span>Coverage: <strong>{productRisk.estimatedDaysRemaining} days</strong></span>
                    </div>
                  </>
                ) : (
                  <p className="text-slate-400">Calculating risk metrics...</p>
                )}
              </div>

              {/* Reorder Recommendation Card */}
              <div className="p-4 bg-slate-950/60 border border-slate-800/80 rounded-xl space-y-2">
                <div className="flex items-center justify-between">
                  <h4 className="text-sm font-semibold text-white">Replenishment Plan</h4>
                  {productReorder && (
                    <Badge variant={productReorder.urgency === 'CRITICAL' ? 'danger' : productReorder.urgency === 'HIGH' ? 'warning' : 'neutral'}>
                      {productReorder.urgency} Urgency
                    </Badge>
                  )}
                </div>
                {productReorder ? (
                  <>
                    <p className="text-slate-300 mt-1">{productReorder.reason}</p>
                    <div className="pt-2 border-t border-slate-800 flex justify-between text-[11px] text-slate-400">
                      <span>Suggested PO: <strong className="text-sky-400">{productReorder.recommendedReorderQuantity} units</strong></span>
                      <span>Est. Cost: <strong className="text-white">${productReorder.estimatedCost.toFixed(2)}</strong></span>
                    </div>
                  </>
                ) : (
                  <p className="text-slate-400">Calculating replenishment quantity...</p>
                )}
              </div>
            </div>

            {/* AI Assistant Explanation Box */}
            <div className="p-4 bg-indigo-950/20 border border-indigo-500/30 rounded-xl space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Bot className="w-4 h-4 text-indigo-400" />
                  <span className="font-semibold text-white">AI Inventory Intelligence Summary</span>
                </div>
                <button
                  onClick={triggerAiExplanation}
                  disabled={isAiLoading}
                  className="px-3 py-1 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white font-medium text-xs flex items-center gap-1.5"
                >
                  <Bot className="w-3.5 h-3.5" />
                  <span>{isAiLoading ? 'Analyzing...' : 'Generate AI Brief'}</span>
                </button>
              </div>

              {aiExplanation ? (
                <div className="p-3 bg-slate-950/80 rounded-lg text-slate-300 whitespace-pre-line leading-relaxed font-sans text-xs border border-indigo-500/20">
                  {aiExplanation}
                </div>
              ) : (
                <p className="text-slate-400 text-xs italic">
                  Click 'Generate AI Brief' to get an executive synthesis of consumption velocity, risk trajectory, and replenishment instructions.
                </p>
              )}
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
