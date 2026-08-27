import React, { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import {
  Bot,
  Sparkles,
  Send,
  ShieldCheck,
  AlertTriangle,
  Boxes,
  TrendingDown,
  CheckCircle,
  Clock,
  Terminal,
  Zap,
  RotateCcw,
} from 'lucide-react';
import { aiApi } from '../services/api';
import { AIDailyBrief, AIChatResponse } from '../types';
import { Badge } from '../components/common/Badge';

interface Message {
  sender: 'user' | 'ai';
  text: string;
  toolsInvoked?: string[];
  timestamp: string;
}

export const AICommandCenterPage: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([
    {
      sender: 'ai',
      text: "Hello! I am StockPilot's AI Inventory Copilot. I analyze your real-time database transactions, demand forecasts, stockout horizons, and anomaly radar through controlled read-only tools.\n\nHow can I assist your supply chain decisions today?",
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    },
  ]);
  const [inputMessage, setInputMessage] = useState('');
  const [conversationId, setConversationId] = useState(`conv-${Date.now()}`);

  // Fetch Executive Daily Brief
  const { data: dailyBrief, isLoading: isBriefLoading, refetch: refetchBrief } = useQuery({
    queryKey: ['ai-daily-brief'],
    queryFn: aiApi.getDailyBrief,
    refetchInterval: 30000,
  });

  // Chat Mutation
  const chatMutation = useMutation({
    mutationFn: aiApi.chat,
    onSuccess: (data: AIChatResponse) => {
      setMessages((prev) => [
        ...prev,
        {
          sender: 'ai',
          text: data.reply,
          toolsInvoked: data.toolsInvoked,
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        },
      ]);
    },
    onError: (err: any) => {
      setMessages((prev) => [
        ...prev,
        {
          sender: 'ai',
          text: 'I was unable to complete the tool execution. Please check backend network connectivity.',
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        },
      ]);
    },
  });

  const handleSendMessage = (textToSend?: string) => {
    const text = textToSend || inputMessage;
    if (!text.trim() || chatMutation.isPending) return;

    const userMsg: Message = {
      sender: 'user',
      text: text.trim(),
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, userMsg]);
    if (!textToSend) setInputMessage('');

    chatMutation.mutate({
      message: text.trim(),
      conversationId,
    });
  };

  const presetPrompts = [
    'What are the top critical stockout risks right now?',
    'Summarize recent warehouse inventory movements.',
    'Explain recommended purchase orders and costs.',
    'Are there any suspicious statistical volume anomalies?',
  ];

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/30 text-indigo-400 text-xs font-semibold mb-2">
            <Sparkles className="w-3.5 h-3.5" />
            <span>Controlled Read-Only Generative AI Intelligence</span>
          </div>
          <h2 className="text-2xl font-bold text-white tracking-tight">AI Command &amp; Operations Center</h2>
          <p className="text-xs text-slate-400 mt-1">
            Authoritative deterministic metrics interpreted through bounded LLM agent tools with zero write permissions.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1.5 text-xs text-slate-400 bg-slate-900 border border-slate-800 px-3 py-1.5 rounded-xl">
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <span>RBAC &amp; Read-Only Guardrails Active</span>
          </div>
        </div>
      </div>

      {/* Executive Daily Brief Card */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-900 via-slate-900 to-indigo-950/40 border border-indigo-500/30 p-6 sm:p-8 shadow-2xl shadow-indigo-500/10">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-indigo-600/30 border border-indigo-500/40 flex items-center justify-center text-indigo-400">
              <Bot className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white tracking-tight">Executive Daily Brief</h3>
              <p className="text-xs text-slate-400">
                Generated at {dailyBrief?.generatedAt ? new Date(dailyBrief.generatedAt).toLocaleString() : 'Live'}
              </p>
            </div>
          </div>
          <button
            onClick={() => refetchBrief()}
            className="p-2 rounded-xl bg-slate-800/80 hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
            title="Refresh Brief"
          >
            <RotateCcw className="w-4 h-4" />
          </button>
        </div>

        {isBriefLoading ? (
          <div className="py-8 text-center text-xs text-slate-400">Synthesizing executive brief...</div>
        ) : dailyBrief ? (
          <div className="space-y-6">
            {/* Deterministic Facts & Calculations Ribbon */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div className="p-3 bg-slate-950/70 border border-slate-800/80 rounded-xl">
                <span className="text-[11px] text-slate-400">Catalog SKUs</span>
                <p className="text-lg font-bold text-white mt-0.5">{dailyBrief.facts.totalProducts}</p>
              </div>
              <div className="p-3 bg-slate-950/70 border border-slate-800/80 rounded-xl">
                <span className="text-[11px] text-slate-400">Inventory Valuation</span>
                <p className="text-lg font-bold text-sky-400 mt-0.5">
                  ${dailyBrief.calculations.totalInventoryValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </p>
              </div>
              <div className="p-3 bg-slate-950/70 border border-slate-800/80 rounded-xl">
                <span className="text-[11px] text-slate-400">Critical Stockout SKUs</span>
                <p className="text-lg font-bold text-rose-400 mt-0.5">{dailyBrief.facts.criticalRiskCount}</p>
              </div>
              <div className="p-3 bg-slate-950/70 border border-slate-800/80 rounded-xl">
                <span className="text-[11px] text-slate-400">Statistical Anomalies</span>
                <p className="text-lg font-bold text-purple-400 mt-0.5">{dailyBrief.facts.anomalyCount}</p>
              </div>
            </div>

            {/* AI Executive Interpretation */}
            <div className="p-5 bg-indigo-950/20 border border-indigo-500/20 rounded-2xl space-y-4 text-xs">
              <div>
                <span className="text-xs font-semibold text-indigo-300 uppercase tracking-wider block mb-1.5">
                  Executive AI Summary
                </span>
                <p className="text-slate-200 leading-relaxed text-sm">
                  {dailyBrief.aiInterpretation.executiveSummary}
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-4 border-t border-indigo-500/20">
                <div>
                  <span className="font-semibold text-rose-400 block mb-2 flex items-center gap-1.5">
                    <AlertTriangle className="w-3.5 h-3.5" />
                    High Priority Risk Alerts
                  </span>
                  <ul className="space-y-1.5 text-slate-300">
                    {dailyBrief.aiInterpretation.highPriorityRisks.map((risk, i) => (
                      <li key={i} className="flex items-start gap-2">
                        <span className="text-rose-400 font-bold">&bull;</span>
                        <span>{risk}</span>
                      </li>
                    ))}
                  </ul>
                </div>

                <div>
                  <span className="font-semibold text-emerald-400 block mb-2 flex items-center gap-1.5">
                    <CheckCircle className="w-3.5 h-3.5" />
                    Recommended Operations Actions
                  </span>
                  <ul className="space-y-1.5 text-slate-300">
                    {dailyBrief.aiInterpretation.recommendedActions.map((action, i) => (
                      <li key={i} className="flex items-start gap-2">
                        <span className="text-emerald-400 font-bold">&bull;</span>
                        <span>{action}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            </div>
          </div>
        ) : null}
      </div>

      {/* Interactive AI Copilot Chat Interface */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl shadow-xl flex flex-col h-[580px] overflow-hidden">
        {/* Chat Header */}
        <div className="px-6 py-4 border-b border-slate-800 bg-slate-900/50 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-xl bg-sky-500/20 text-sky-400 border border-sky-500/30 flex items-center justify-center">
              <Bot className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-white">StockPilot AI Natural Language Copilot</h3>
              <p className="text-[11px] text-slate-400">Contextual supply chain assistant powered by deterministic backend tools</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
            <span className="text-[11px] text-slate-400">11 Tools Online</span>
          </div>
        </div>

        {/* Chat History */}
        <div className="flex-1 p-6 overflow-y-auto space-y-4">
          {messages.map((msg, index) => (
            <div key={index} className={`flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'}`}>
              <div
                className={`max-w-2xl rounded-2xl p-4 text-xs ${
                  msg.sender === 'user'
                    ? 'bg-sky-600 text-white rounded-tr-none'
                    : 'bg-slate-950 border border-slate-800 text-slate-200 rounded-tl-none space-y-2'
                }`}
              >
                <div className="whitespace-pre-line leading-relaxed">{msg.text}</div>

                {msg.toolsInvoked && msg.toolsInvoked.length > 0 && (
                  <div className="pt-2 border-t border-slate-800 flex items-center gap-1.5 flex-wrap">
                    <Terminal className="w-3 h-3 text-sky-400 shrink-0" />
                    <span className="text-[10px] text-slate-400">Invoked Tools:</span>
                    {msg.toolsInvoked.map((tool, tIdx) => (
                      <span key={tIdx} className="px-2 py-0.5 rounded bg-slate-900 font-mono text-[10px] text-sky-400 border border-slate-800">
                        {tool}
                      </span>
                    ))}
                  </div>
                )}

                <div className={`text-[10px] ${msg.sender === 'user' ? 'text-sky-200' : 'text-slate-500'} text-right mt-1`}>
                  {msg.timestamp}
                </div>
              </div>
            </div>
          ))}

          {chatMutation.isPending && (
            <div className="flex justify-start">
              <div className="bg-slate-950 border border-slate-800 rounded-2xl rounded-tl-none p-4 text-xs text-slate-400 flex items-center gap-2">
                <div className="w-3.5 h-3.5 border-2 border-sky-500 border-t-transparent rounded-full animate-spin"></div>
                <span>Executing read-only tools and evaluating context...</span>
              </div>
            </div>
          )}
        </div>

        {/* Preset Prompts Ribbon */}
        <div className="px-6 py-2 bg-slate-950/60 border-t border-slate-800/80 flex items-center gap-2 overflow-x-auto">
          <span className="text-[10px] text-slate-500 shrink-0">Quick Queries:</span>
          {presetPrompts.map((prompt, pIdx) => (
            <button
              key={pIdx}
              onClick={() => handleSendMessage(prompt)}
              className="px-2.5 py-1 rounded-lg bg-slate-900 hover:bg-slate-800 text-slate-300 hover:text-white text-[11px] border border-slate-800 shrink-0 transition-colors"
            >
              {prompt}
            </button>
          ))}
        </div>

        {/* Input Bar */}
        <div className="p-4 border-t border-slate-800 bg-slate-900/60">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSendMessage();
            }}
            className="flex items-center gap-3"
          >
            <input
              type="text"
              placeholder="Ask anything about stock velocity, depletion risks, warehouse transfers, or purchase suggestions..."
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              className="flex-1 bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-sky-500"
            />
            <button
              type="submit"
              disabled={!inputMessage.trim() || chatMutation.isPending}
              className="px-4 py-2.5 rounded-xl bg-sky-500 hover:bg-sky-400 text-white font-semibold text-xs disabled:opacity-40 shadow-lg shadow-sky-500/20 flex items-center gap-1.5 transition-all"
            >
              <Send className="w-3.5 h-3.5" />
              <span>Ask AI</span>
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};
