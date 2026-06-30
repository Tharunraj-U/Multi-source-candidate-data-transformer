import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function formatConfidence(score: number | null | undefined): string {
  if (score == null) return '—'
  return `${Math.round(score * 100)}%`
}

export function confidenceColor(score: number | null | undefined): string {
  if (score == null) return 'bg-slate-100 text-slate-600'
  if (score >= 0.8) return 'bg-emerald-100 text-emerald-700'
  if (score >= 0.6) return 'bg-amber-100 text-amber-700'
  return 'bg-red-100 text-red-700'
}

export function statusColor(status: string): string {
  switch (status) {
    case 'COMPLETED':
      return 'bg-emerald-100 text-emerald-700'
    case 'PROCESSING':
      return 'bg-blue-100 text-blue-700'
    case 'PARTIAL':
      return 'bg-amber-100 text-amber-700'
    case 'FAILED':
      return 'bg-red-100 text-red-700'
    default:
      return 'bg-slate-100 text-slate-600'
  }
}
