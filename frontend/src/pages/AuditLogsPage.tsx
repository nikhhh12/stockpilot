import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  ShieldCheck,
  Search,
  Filter,
  User,
  RefreshCw,
  Eye,
  Calendar,
  Lock,
  Layers,
} from 'lucide-react';
import { auditLogApi } from '../services/api';
import { AuditLog } from '../types';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';
import { useAuth } from '../context/AuthContext';

export const AuditLogsPage: React.FC = () => {
  const { user } = useAuth();
  const [action, setAction] = useState<string>('');
  const [entityType, setEntityType] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null);

  const isAuthorized = user?.role === 'ADMIN' || user?.role === 'INVENTORY_MANAGER';

  const {
    data: auditData,
    isLoading,
    refetch,
    isFetching,
  } = useQuery({
    queryKey: ['audit-logs', action, entityType, page],
    queryFn: () =>
      auditLogApi.getPaged({
        action: action || undefined,
        entityType: entityType || undefined,
        page,
        size: 15,
      }),
    enabled: isAuthorized,
  });

  const getActionBadge = (act: string) => {
    const upper = act.toUpperCase();
    if (upper.includes('CREATE') || upper.includes('STOCK_IN') || upper.includes('REGISTER')) {
      return <Badge variant="success">{act}</Badge>;
    }
    if (upper.includes('DELETE') || upper.includes('DAMAGE') || upper.includes('CANCEL')) {
      return <Badge variant="danger">{act}</Badge>;
    }
    if (upper.includes('UPDATE') || upper.includes('TRANSFER') || upper.includes('RESERVE')) {
      return <Badge variant="warning">{act}</Badge>;
    }
    if (upper.includes('LOGIN') || upper.includes('AUTH')) {
      return <Badge variant="info">{act}</Badge>;
    }
    return <Badge variant="purple">{act}</Badge>;
  };

  if (!isAuthorized) {
    return (
      <div className="p-8 text-center bg-slate-900/50 border border-slate-800 rounded-2xl max-w-lg mx-auto mt-12 space-y-4">
        <Lock className="w-12 h-12 text-amber-400 mx-auto" />
        <h3 className="text-lg font-bold text-white">Privileged Access Required</h3>
        <p className="text-xs text-slate-400">
          System audit logs are restricted to Administrators and Inventory Managers under the StockPilot security policy.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-white tracking-tight flex items-center gap-2.5">
            <ShieldCheck className="w-6 h-6 text-sky-400" />
            <span>System Audit Trail</span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Immutable administrative and operational audit records capturing user authentication, entity mutations, and governance events.
          </p>
        </div>
        <button
          onClick={() => refetch()}
          disabled={isFetching}
          className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold transition-all border border-slate-700/60 shadow-sm"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isFetching ? 'animate-spin text-sky-400' : ''}`} />
          <span>Refresh Feed</span>
        </button>
      </div>

      {/* Filter Bar */}
      <div className="p-4 bg-slate-900/80 border border-slate-800 rounded-2xl flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-3 w-full flex-wrap">
          <select
            value={action}
            onChange={(e) => {
              setAction(e.target.value);
              setPage(0);
            }}
            className="bg-slate-950/80 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-sky-500"
          >
            <option value="">All Action Types</option>
            <option value="LOGIN">LOGIN</option>
            <option value="REGISTER">REGISTER</option>
            <option value="CREATE_PRODUCT">CREATE_PRODUCT</option>
            <option value="UPDATE_PRODUCT">UPDATE_PRODUCT</option>
            <option value="DELETE_PRODUCT">DELETE_PRODUCT</option>
            <option value="STOCK_IN">STOCK_IN</option>
            <option value="STOCK_OUT">STOCK_OUT</option>
            <option value="RESERVE">RESERVE</option>
            <option value="RELEASE">RELEASE</option>
            <option value="TRANSFER">TRANSFER</option>
            <option value="PO_RECEIVE">PO_RECEIVE</option>
            <option value="APPROVE_PO">APPROVE_PO</option>
          </select>

          <select
            value={entityType}
            onChange={(e) => {
              setEntityType(e.target.value);
              setPage(0);
            }}
            className="bg-slate-950/80 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-sky-500"
          >
            <option value="">All Entities</option>
            <option value="Product">Product</option>
            <option value="Inventory">Inventory</option>
            <option value="PurchaseOrder">PurchaseOrder</option>
            <option value="User">User</option>
            <option value="Category">Category</option>
            <option value="Warehouse">Warehouse</option>
            <option value="Supplier">Supplier</option>
          </select>

          {(action || entityType) && (
            <button
              onClick={() => {
                setAction('');
                setEntityType('');
                setPage(0);
              }}
              className="px-3 py-2 rounded-xl bg-slate-800 text-xs text-slate-300 hover:text-white transition-colors"
            >
              Clear Filters
            </button>
          )}
        </div>
      </div>

      {/* Audit Logs Table */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-900/50 text-slate-400 font-medium">
                <th className="p-4">Event ID &amp; Time</th>
                <th className="p-4">Action</th>
                <th className="p-4">Target Entity</th>
                <th className="p-4">Description</th>
                <th className="p-4">Actor</th>
                <th className="p-4 text-right">Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-slate-400">
                    Loading security audit logs...
                  </td>
                </tr>
              ) : auditData?.content.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-slate-400">
                    No audit records matching query filters.
                  </td>
                </tr>
              ) : (
                auditData?.content.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="p-4 font-mono text-slate-400">
                      <div className="font-semibold text-white">#{log.id}</div>
                      <div className="text-[10px] text-slate-500">
                        {new Date(log.createdAt).toLocaleDateString()}{' '}
                        {new Date(log.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </div>
                    </td>
                    <td className="p-4">{getActionBadge(log.action)}</td>
                    <td className="p-4">
                      {log.entityType ? (
                        <div className="flex items-center gap-1.5 text-slate-300">
                          <Layers className="w-3.5 h-3.5 text-slate-500" />
                          <span className="font-medium">{log.entityType}</span>
                          {log.entityId && (
                            <span className="font-mono text-sky-400 text-[11px]">#{log.entityId}</span>
                          )}
                        </div>
                      ) : (
                        <span className="text-slate-500 italic">Global / System</span>
                      )}
                    </td>
                    <td className="p-4 text-slate-300 max-w-xs truncate" title={log.description}>
                      {log.description || 'System operation executed successfully'}
                    </td>
                    <td className="p-4">
                      <div className="flex items-center gap-1.5">
                        <User className="w-3 h-3 text-slate-500" />
                        <span className="text-slate-200 font-medium">
                          {log.userName || log.userEmail || (log.userId ? `User #${log.userId}` : 'System')}
                        </span>
                      </div>
                      {log.userEmail && (
                        <div className="text-[10px] text-slate-500 pl-4">{log.userEmail}</div>
                      )}
                    </td>
                    <td className="p-4 text-right">
                      <button
                        onClick={() => setSelectedLog(log)}
                        className="p-1.5 rounded-lg text-slate-400 hover:text-sky-400 hover:bg-sky-500/10 transition-colors"
                        title="View Audit Record"
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
        {auditData && auditData.totalPages > 1 && (
          <div className="p-4 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
            <span>
              Showing {auditData.pageNumber * auditData.pageSize + 1} -{' '}
              {Math.min((auditData.pageNumber + 1) * auditData.pageSize, auditData.totalElements)} of{' '}
              {auditData.totalElements} records
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
                Page {page + 1} of {auditData.totalPages}
              </span>
              <button
                disabled={auditData.last}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1 rounded-lg bg-slate-800 text-white disabled:opacity-40"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Audit Detail Modal */}
      {selectedLog && (
        <Modal
          isOpen={!!selectedLog}
          onClose={() => setSelectedLog(null)}
          title={`Audit Log Record #${selectedLog.id}`}
          subtitle={`Event: ${selectedLog.action} • ${new Date(selectedLog.createdAt).toLocaleString()}`}
          maxWidth="md"
        >
          <div className="space-y-4 text-xs">
            <div className="p-3 bg-slate-950 border border-slate-800 rounded-xl space-y-2">
              <div className="flex justify-between">
                <span className="text-slate-400">Action:</span>
                <span className="font-semibold">{getActionBadge(selectedLog.action)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Target Entity:</span>
                <span className="text-white font-medium">
                  {selectedLog.entityType || 'None'} {selectedLog.entityId ? `(#${selectedLog.entityId})` : ''}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Recorded At:</span>
                <span className="font-mono text-slate-300">
                  {new Date(selectedLog.createdAt).toISOString()}
                </span>
              </div>
            </div>

            <div className="p-3 bg-slate-950 border border-slate-800 rounded-xl space-y-2">
              <div className="flex justify-between">
                <span className="text-slate-400">Actor ID:</span>
                <span className="font-mono text-sky-400">{selectedLog.userId ?? 'N/A'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Actor Name:</span>
                <span className="text-white">{selectedLog.userName || 'System / Automated'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Actor Email:</span>
                <span className="text-slate-300 font-mono">{selectedLog.userEmail || 'N/A'}</span>
              </div>
            </div>

            <div className="p-3 bg-slate-950 border border-slate-800 rounded-xl space-y-1">
              <p className="text-slate-400 font-semibold mb-1">Event Description &amp; Context:</p>
              <p className="text-slate-200 leading-relaxed whitespace-pre-wrap">
                {selectedLog.description || 'No additional contextual metadata provided.'}
              </p>
            </div>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setSelectedLog(null)}
                className="px-4 py-2 rounded-xl bg-slate-800 text-white hover:bg-slate-700 font-medium"
              >
                Close Audit Record
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
