import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  ArrowLeftRight,
  Search,
  Filter,
  Calendar,
  Warehouse,
  User,
  ArrowDownLeft,
  ArrowUpRight,
  RefreshCw,
  ShieldCheck,
  Eye,
} from 'lucide-react';
import { transactionApi, productApi, warehouseApi } from '../services/api';
import { StockTransaction, TransactionType } from '../types';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';

export const TransactionsPage: React.FC = () => {
  const [productId, setProductId] = useState<number | undefined>();
  const [warehouseId, setWarehouseId] = useState<number | undefined>();
  const [type, setType] = useState<string>('');
  const [page, setPage] = useState(0);

  const [selectedTx, setSelectedTx] = useState<StockTransaction | null>(null);

  const { data: txData, isLoading } = useQuery({
    queryKey: ['transactions', productId, warehouseId, type, page],
    queryFn: () =>
      transactionApi.getPaged({
        productId,
        warehouseId,
        type: type || undefined,
        page,
        size: 15,
      }),
  });

  const { data: products } = useQuery({ queryKey: ['products-all'], queryFn: productApi.getAll });
  const { data: warehouses } = useQuery({ queryKey: ['warehouses-all'], queryFn: warehouseApi.getAll });

  const getTransactionBadge = (t: TransactionType) => {
    switch (t) {
      case 'STOCK_IN':
      case 'TRANSFER_IN':
      case 'RETURN':
        return <Badge variant="success">{t}</Badge>;
      case 'STOCK_OUT':
      case 'TRANSFER_OUT':
        return <Badge variant="danger">{t}</Badge>;
      case 'RESERVE':
        return <Badge variant="warning">{t}</Badge>;
      case 'RELEASE':
        return <Badge variant="info">{t}</Badge>;
      case 'DAMAGE':
        return <Badge variant="danger">{t}</Badge>;
      case 'ADJUSTMENT':
      default:
        return <Badge variant="purple">{t}</Badge>;
    }
  };

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-white tracking-tight flex items-center gap-2.5">
            <ArrowLeftRight className="w-6 h-6 text-sky-400" />
            <span>Audit Transaction Ledger</span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Immutable, append-only physical stock transaction history with audit metadata and user attribution.
          </p>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="p-4 bg-slate-900/80 border border-slate-800 rounded-2xl flex flex-col md:flex-row items-center gap-4">
        <div className="flex items-center gap-3 w-full flex-wrap">
          <select
            value={type}
            onChange={(e) => {
              setType(e.target.value);
              setPage(0);
            }}
            className="bg-slate-950/80 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-sky-500"
          >
            <option value="">All Transaction Types</option>
            <option value="STOCK_IN">STOCK_IN (Receipt)</option>
            <option value="STOCK_OUT">STOCK_OUT (Fulfillment)</option>
            <option value="RESERVE">RESERVE (Allocation)</option>
            <option value="RELEASE">RELEASE (Unreserve)</option>
            <option value="ADJUSTMENT">ADJUSTMENT (Count correction)</option>
            <option value="TRANSFER_OUT">TRANSFER_OUT</option>
            <option value="TRANSFER_IN">TRANSFER_IN</option>
            <option value="DAMAGE">DAMAGE (Quarantine)</option>
            <option value="RETURN">RETURN</option>
          </select>

          <select
            value={productId || ''}
            onChange={(e) => {
              setProductId(e.target.value ? Number(e.target.value) : undefined);
              setPage(0);
            }}
            className="bg-slate-950/80 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-sky-500 max-w-[200px]"
          >
            <option value="">All Products</option>
            {products?.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>

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
        </div>
      </div>

      {/* Transactions Table */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-900/50 text-slate-400 font-medium">
                <th className="p-4">Tx ID &amp; Time</th>
                <th className="p-4">Product / SKU</th>
                <th className="p-4">Warehouse</th>
                <th className="p-4 text-center">Type</th>
                <th className="p-4 text-center">Quantity Delta</th>
                <th className="p-4 text-center">Prev &rarr; New</th>
                <th className="p-4">Reference &amp; Reason</th>
                <th className="p-4">Actor</th>
                <th className="p-4 text-right">Inspect</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {isLoading ? (
                <tr>
                  <td colSpan={9} className="p-8 text-center text-slate-400">
                    Loading ledger transactions...
                  </td>
                </tr>
              ) : txData?.content.length === 0 ? (
                <tr>
                  <td colSpan={9} className="p-8 text-center text-slate-400">
                    No transactions recorded matching filters.
                  </td>
                </tr>
              ) : (
                txData?.content.map((tx) => (
                  <tr key={tx.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="p-4 font-mono text-slate-400">
                      <div className="font-semibold text-white">#{tx.id}</div>
                      <div className="text-[10px] text-slate-500">
                        {new Date(tx.createdAt).toLocaleDateString()} {new Date(tx.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </div>
                    </td>
                    <td className="p-4">
                      <div className="font-semibold text-white truncate max-w-[180px]">
                        {tx.product?.name || tx.productName || 'Product #' + (tx.productId || '')}
                      </div>
                      <div className="text-[11px] font-mono text-sky-400">
                        {tx.product?.sku || tx.productSku || 'SKU-N/A'}
                      </div>
                    </td>
                    <td className="p-4 text-slate-300">
                      <div>{tx.warehouse?.name || tx.warehouseName || 'Warehouse #' + (tx.warehouseId || '')}</div>
                      <div className="text-[10px] font-mono text-slate-500">{tx.warehouse?.code || ''}</div>
                    </td>
                    <td className="p-4 text-center">{getTransactionBadge(tx.transactionType)}</td>
                    <td className="p-4 text-center font-bold text-sm">
                      {tx.transactionType === 'STOCK_IN' || tx.transactionType === 'TRANSFER_IN' || tx.transactionType === 'RETURN' ? (
                        <span className="text-emerald-400">+{tx.quantity}</span>
                      ) : tx.transactionType === 'STOCK_OUT' || tx.transactionType === 'TRANSFER_OUT' || tx.transactionType === 'DAMAGE' ? (
                        <span className="text-rose-400">-{tx.quantity}</span>
                      ) : (
                        <span className="text-amber-400">Δ {tx.quantity}</span>
                      )}
                    </td>
                    <td className="p-4 text-center font-mono text-slate-400">
                      <div className="text-xs">
                        Avail: {tx.previousAvailableQuantity ?? tx.previousQuantity ?? 0} &rarr; <strong className="text-white">{tx.newAvailableQuantity ?? tx.newQuantity ?? 0}</strong>
                      </div>
                      {(tx.previousReservedQuantity !== undefined && tx.newReservedQuantity !== undefined && tx.previousReservedQuantity !== tx.newReservedQuantity) && (
                        <div className="text-[10px] text-amber-400">
                          Res: {tx.previousReservedQuantity} &rarr; {tx.newReservedQuantity}
                        </div>
                      )}
                      {(tx.previousDamagedQuantity !== undefined && tx.newDamagedQuantity !== undefined && tx.previousDamagedQuantity !== tx.newDamagedQuantity) && (
                        <div className="text-[10px] text-rose-400">
                          Dam: {tx.previousDamagedQuantity} &rarr; {tx.newDamagedQuantity}
                        </div>
                      )}
                    </td>
                    <td className="p-4">
                      {tx.referenceNumber && (
                        <div className="font-mono text-sky-400 text-[11px]">{tx.referenceNumber}</div>
                      )}
                      <div className="text-slate-400 truncate max-w-[150px]">{tx.reason || 'General Stock Operation'}</div>
                    </td>
                    <td className="p-4 text-slate-300">
                      <div className="flex items-center gap-1.5">
                        <User className="w-3 h-3 text-slate-500" />
                        <span>{tx.performedBy?.name || tx.performedByName || 'System Admin'}</span>
                      </div>
                    </td>
                    <td className="p-4 text-right">
                      <button
                        onClick={() => setSelectedTx(tx)}
                        className="p-1.5 rounded-lg text-slate-400 hover:text-sky-400 hover:bg-sky-500/10 transition-colors"
                      >
                        <Eye className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {txData && txData.totalPages > 1 && (
          <div className="p-4 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
            <span>
              Showing {txData.pageNumber * txData.pageSize + 1} -{' '}
              {Math.min((txData.pageNumber + 1) * txData.pageSize, txData.totalElements)} of {txData.totalElements} transactions
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
                Page {page + 1} of {txData.totalPages}
              </span>
              <button
                disabled={txData.last}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1 rounded-lg bg-slate-800 text-white disabled:opacity-40"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Transaction Detail Modal */}
      {selectedTx && (
        <Modal
          isOpen={!!selectedTx}
          onClose={() => setSelectedTx(null)}
          title={`Transaction Audit Record #${selectedTx.id}`}
          subtitle={`Type: ${selectedTx.transactionType} • Timestamp: ${new Date(selectedTx.createdAt).toLocaleString()}`}
          maxWidth="md"
        >
          <div className="space-y-4 text-xs">
            <div className="p-3 bg-slate-950 border border-slate-800 rounded-xl space-y-2">
              <div className="flex justify-between">
                <span className="text-slate-400">Product SKU:</span>
                <span className="font-mono text-sky-400 font-bold">{selectedTx.product?.sku || selectedTx.productSku || 'N/A'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Product Name:</span>
                <span className="text-white font-semibold">{selectedTx.product?.name || selectedTx.productName || 'N/A'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Warehouse Location:</span>
                <span className="text-white">
                  {selectedTx.warehouse?.name || selectedTx.warehouseName || 'Warehouse #' + selectedTx.warehouseId}
                </span>
              </div>
            </div>

            <div className="p-3 bg-slate-950 border border-slate-800 rounded-xl space-y-2">
              <div className="flex justify-between">
                <span className="text-slate-400">Quantity Transacted:</span>
                <span className="font-bold text-white text-sm">{selectedTx.quantity} units</span>
              </div>
              <div className="pt-2 border-t border-slate-800/80 space-y-1.5 font-mono">
                <div className="flex justify-between">
                  <span className="text-slate-400 font-sans">Available Quantity:</span>
                  <span className="text-sky-400 font-bold">
                    {selectedTx.previousAvailableQuantity ?? selectedTx.previousQuantity ?? 0} &rarr; {selectedTx.newAvailableQuantity ?? selectedTx.newQuantity ?? 0}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400 font-sans">Reserved Quantity:</span>
                  <span className="text-amber-400">
                    {selectedTx.previousReservedQuantity ?? 0} &rarr; {selectedTx.newReservedQuantity ?? 0}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400 font-sans">Damaged Quantity:</span>
                  <span className="text-rose-400">
                    {selectedTx.previousDamagedQuantity ?? 0} &rarr; {selectedTx.newDamagedQuantity ?? 0}
                  </span>
                </div>
              </div>
            </div>

            <div className="p-3 bg-slate-950 border border-slate-800 rounded-xl space-y-2">
              <div className="flex justify-between">
                <span className="text-slate-400">Reference Number:</span>
                <span className="font-mono text-white">{selectedTx.referenceNumber || 'N/A'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Reason / Operation:</span>
                <span className="text-slate-300">{selectedTx.reason || 'General Ledger Entry'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Performed By:</span>
                <span className="text-slate-300">{selectedTx.performedBy?.name || selectedTx.performedByName || 'System'}</span>
              </div>
              {selectedTx.notes && (
                <div className="pt-2 border-t border-slate-800 text-slate-400">
                  <p className="font-semibold mb-0.5">Notes:</p>
                  <p>{selectedTx.notes}</p>
                </div>
              )}
            </div>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setSelectedTx(null)}
                className="px-4 py-2 rounded-xl bg-slate-800 text-white hover:bg-slate-700"
              >
                Close Record
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
