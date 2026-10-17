import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Boxes,
  PackageSearch,
  ArrowLeftRight,
  ShoppingCart,
  Truck,
  Warehouse as WarehouseIcon,
  TrendingUp,
  Bot,
  Settings,
  ShieldCheck,
  LogOut,
  Sparkles,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export const Sidebar: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const navItems = [
    { to: '/', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/products', label: 'Products', icon: Boxes },
    { to: '/inventory', label: 'Inventory', icon: PackageSearch },
    { to: '/transactions', label: 'Transactions', icon: ArrowLeftRight },
    { to: '/purchase-orders', label: 'Purchase Orders', icon: ShoppingCart },
    { to: '/analytics', label: 'Analytics & Forecasts', icon: TrendingUp },
    { to: '/ai-command-center', label: 'AI Command Center', icon: Bot, highlight: true },
    { to: '/suppliers', label: 'Suppliers', icon: Truck },
    { to: '/warehouses', label: 'Warehouses', icon: WarehouseIcon },
    { to: '/settings', label: 'Settings', icon: Settings },
    { to: '/audit-logs', label: 'Audit Logs', icon: ShieldCheck },
  ];

  return (
    <aside className="w-64 bg-slate-900/90 border-r border-slate-800 flex flex-col shrink-0 min-h-screen backdrop-blur-xl">
      {/* Brand Header */}
      <div className="p-5 border-b border-slate-800/80 flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-sky-600 to-indigo-500 border border-sky-400/30 flex items-center justify-center text-white font-black text-lg shadow-lg shadow-sky-500/20">
          SP
        </div>
        <div>
          <h1 className="text-base font-bold text-white tracking-tight flex items-center gap-1.5">
            StockPilot <span className="text-xs px-1.5 py-0.5 rounded bg-sky-500/20 text-sky-400 font-mono font-semibold border border-sky-500/30">AI</span>
          </h1>
          <p className="text-[11px] text-slate-400">Inventory Intelligence</p>
        </div>
      </div>

      {/* Navigation Links */}
      <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 ${
                  isActive
                    ? 'bg-sky-500/15 text-sky-400 border border-sky-500/30 shadow-[0_0_15px_rgba(14,165,233,0.15)] font-semibold'
                    : item.highlight
                    ? 'text-indigo-300 hover:text-white hover:bg-indigo-500/10 border border-indigo-500/20'
                    : 'text-slate-400 hover:text-slate-100 hover:bg-slate-800/60'
                }`
              }
            >
              <Icon className={`w-4 h-4 shrink-0 ${item.highlight ? 'text-indigo-400' : ''}`} />
              <span className="flex-1">{item.label}</span>
              {item.highlight && (
                <span className="flex items-center gap-1 text-[10px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                  <Sparkles className="w-2.5 h-2.5" />
                  AI
                </span>
              )}
            </NavLink>
          );
        })}
      </nav>

      {/* User Footer */}
      <div className="p-4 border-t border-slate-800/80 bg-slate-900/40">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5 overflow-hidden">
            <div className="w-8 h-8 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-xs font-bold text-slate-300">
              {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
            </div>
            <div className="truncate">
              <p className="text-xs font-semibold text-white truncate">{user?.name || 'User'}</p>
              <p className="text-[10px] text-slate-400 truncate">{user?.role || 'STAFF'}</p>
            </div>
          </div>
          <button
            onClick={logout}
            title="Log Out"
            className="p-1.5 text-slate-400 hover:text-rose-400 rounded-lg hover:bg-rose-500/10 transition-colors"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </aside>
  );
};
