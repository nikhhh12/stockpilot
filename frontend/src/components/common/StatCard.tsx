import React from 'react';
import { LucideIcon } from 'lucide-react';

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: LucideIcon;
  colorScheme?: 'blue' | 'emerald' | 'amber' | 'rose' | 'indigo' | 'purple';
  badge?: {
    text: string;
    variant: 'success' | 'warning' | 'danger' | 'info' | 'purple' | 'neutral';
  };
  onClick?: () => void;
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  subtitle,
  icon: Icon,
  colorScheme = 'blue',
  badge,
  onClick,
}) => {
  const schemeStyles = {
    blue: {
      bg: 'bg-sky-500/10',
      border: 'border-sky-500/20',
      icon: 'text-sky-400',
      glow: 'group-hover:border-sky-500/40',
    },
    emerald: {
      bg: 'bg-emerald-500/10',
      border: 'border-emerald-500/20',
      icon: 'text-emerald-400',
      glow: 'group-hover:border-emerald-500/40',
    },
    amber: {
      bg: 'bg-amber-500/10',
      border: 'border-amber-500/20',
      icon: 'text-amber-400',
      glow: 'group-hover:border-amber-500/40',
    },
    rose: {
      bg: 'bg-rose-500/10',
      border: 'border-rose-500/20',
      icon: 'text-rose-400',
      glow: 'group-hover:border-rose-500/40',
    },
    indigo: {
      bg: 'bg-indigo-500/10',
      border: 'border-indigo-500/20',
      icon: 'text-indigo-400',
      glow: 'group-hover:border-indigo-500/40',
    },
    purple: {
      bg: 'bg-purple-500/10',
      border: 'border-purple-500/20',
      icon: 'text-purple-400',
      glow: 'group-hover:border-purple-500/40',
    },
  };

  const current = schemeStyles[colorScheme];

  return (
    <div
      onClick={onClick}
      className={`group relative bg-slate-900/70 backdrop-blur-md border border-slate-800/80 rounded-2xl p-5 shadow-lg transition-all duration-300 hover:shadow-xl hover:-translate-y-0.5 ${
        current.glow
      } ${onClick ? 'cursor-pointer' : ''}`}
    >
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-medium uppercase tracking-wider text-slate-400">{title}</p>
          <h3 className="text-2xl font-bold text-white mt-1 tracking-tight">{value}</h3>
          {subtitle && <p className="text-xs text-slate-400 mt-1">{subtitle}</p>}
        </div>
        <div className={`p-3 rounded-xl border ${current.bg} ${current.border} ${current.icon} transition-transform group-hover:scale-105`}>
          <Icon className="w-5 h-5" />
        </div>
      </div>
      {badge && (
        <div className="mt-4 pt-3 border-t border-slate-800/50 flex items-center justify-between text-xs">
          <span className="text-slate-400">Status</span>
          <span className="font-medium text-slate-200">{badge.text}</span>
        </div>
      )}
    </div>
  );
};
