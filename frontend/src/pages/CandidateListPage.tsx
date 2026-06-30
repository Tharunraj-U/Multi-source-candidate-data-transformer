import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Eye, Filter, RefreshCw, Search, Trash2, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { deleteCandidate, listCandidates, reprocessCandidate } from '../api/client'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/Card'
import { Alert, EmptyState, Spinner } from '../components/ui/Feedback'
import { EmailValidationIcon } from '../components/EmailValidation'
import { Input, Select } from '../components/ui/Input'
import type { CandidateStatus } from '../types/candidate'
import { confidenceColor, formatConfidence, formatDate, statusColor } from '../lib/utils'
import { pictureUrl } from '../api/client'

const STATUS_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: 'All statuses' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'PROCESSING', label: 'Processing' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'PARTIAL', label: 'Partial' },
  { value: 'FAILED', label: 'Failed' },
]

export function CandidateListPage() {
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [minConfidence, setMinConfidence] = useState('')
  const [company, setCompany] = useState('')
  const [status, setStatus] = useState<CandidateStatus | ''>('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [sort, setSort] = useState('updatedAt,desc')
  const [actionError, setActionError] = useState<string>()

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search), 300)
    return () => clearTimeout(timer)
  }, [search])

  useEffect(() => {
    setPage(0)
  }, [debouncedSearch, minConfidence, company, status, size, sort])

  const { data, isLoading, error } = useQuery({
    queryKey: ['candidates', debouncedSearch, minConfidence, company, status, page, size, sort],
    queryFn: () =>
      listCandidates({
        search: debouncedSearch || undefined,
        minConfidence: minConfidence ? Number(minConfidence) : undefined,
        company: company || undefined,
        status: status || undefined,
        page,
        size,
        sort,
      }),
  })

  const deleteMutation = useMutation({
    mutationFn: deleteCandidate,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['candidates'] })
      setActionError(undefined)
    },
    onError: (err: Error) => setActionError(err.message),
  })

  const reprocessMutation = useMutation({
    mutationFn: reprocessCandidate,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['candidates'] })
      setActionError(undefined)
    },
    onError: (err: Error) => setActionError(err.message),
  })

  const toggleSort = (field: string) => {
    const [currentField, currentDir] = sort.split(',')
    if (currentField === field) {
      setSort(`${field},${currentDir === 'asc' ? 'desc' : 'asc'}`)
    } else {
      setSort(`${field},desc`)
    }
  }

  const sortIndicator = (field: string) => {
    const [currentField, dir] = sort.split(',')
    if (currentField !== field) return ''
    return dir === 'asc' ? ' ↑' : ' ↓'
  }

  const hasActiveFilters = Boolean(debouncedSearch || minConfidence || company || status)

  const clearFilters = () => {
    setSearch('')
    setDebouncedSearch('')
    setMinConfidence('')
    setCompany('')
    setStatus('')
    setPage(0)
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">Candidates</h1>
        <p className="mt-1 text-muted-foreground">Search, filter, and manage canonical candidate profiles.</p>
      </div>

      {actionError && <Alert message={actionError} variant="error" />}
      {error && <Alert message={(error as Error).message} variant="error" />}

      <Card>
        <CardHeader className="flex flex-row items-center justify-between gap-4 py-4">
          <div className="flex items-center gap-2">
            <Filter className="h-4 w-4 text-brand-600" />
            <CardTitle className="text-base">Filters</CardTitle>
            {hasActiveFilters && (
              <span className="rounded-full bg-brand-100 px-2 py-0.5 text-xs font-medium text-brand-700">
                Active
              </span>
            )}
          </div>
          {hasActiveFilters && (
            <Button variant="ghost" size="sm" onClick={clearFilters} className="text-muted-foreground">
              <X className="h-4 w-4" />
              Clear all
            </Button>
          )}
        </CardHeader>
        <CardContent className="space-y-4 pt-2">
          <div className="grid gap-4 lg:grid-cols-12">
            <div className="lg:col-span-6">
              <Input
                label="Search"
                type="search"
                placeholder="Search name, email, company…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                leftIcon={<Search className="h-4 w-4" />}
              />
            </div>
            <div className="lg:col-span-3">
              <Input
                label="Min confidence"
                type="number"
                min={0}
                max={1}
                step={0.1}
                placeholder="0.0 – 1.0"
                value={minConfidence}
                onChange={(e) => setMinConfidence(e.target.value)}
              />
            </div>
            <div className="lg:col-span-3">
              <Input
                label="Company"
                placeholder="Filter by company"
                value={company}
                onChange={(e) => setCompany(e.target.value)}
              />
            </div>
          </div>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-12">
            <div className="lg:col-span-3">
              <Select
                label="Status"
                value={status}
                onChange={(e) => setStatus(e.target.value as CandidateStatus | '')}
              >
                {STATUS_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </Select>
            </div>
            <div className="lg:col-span-3">
              <Select label="Page size" value={size} onChange={(e) => setSize(Number(e.target.value))}>
                <option value={20}>20 per page</option>
                <option value={50}>50 per page</option>
                <option value={100}>100 per page</option>
              </Select>
            </div>
            {data && (
              <div className="flex items-end lg:col-span-6 lg:justify-end">
                <p className="pb-2.5 text-sm text-muted-foreground">
                  {data.totalElements} candidate{data.totalElements === 1 ? '' : 's'} found
                </p>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="flex justify-center py-16">
              <Spinner />
            </div>
          ) : !data?.content.length ? (
            <EmptyState
              title="No candidates found"
              description="Upload a candidate to get started, or adjust your filters."
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-border bg-slate-50">
                    <th className="px-4 py-3 font-medium text-muted-foreground">Photo</th>
                    <th
                      className="cursor-pointer px-4 py-3 font-medium text-muted-foreground hover:text-foreground"
                      onClick={() => toggleSort('fullName')}
                    >
                      Name{sortIndicator('fullName')}
                    </th>
                    <th className="px-4 py-3 font-medium text-muted-foreground">Email</th>
                    <th className="px-4 py-3 font-medium text-muted-foreground">Company</th>
                    <th
                      className="cursor-pointer px-4 py-3 font-medium text-muted-foreground hover:text-foreground"
                      onClick={() => toggleSort('yearsExperience')}
                    >
                      Experience{sortIndicator('yearsExperience')}
                    </th>
                    <th
                      className="cursor-pointer px-4 py-3 font-medium text-muted-foreground hover:text-foreground"
                      onClick={() => toggleSort('overallConfidence')}
                    >
                      Confidence{sortIndicator('overallConfidence')}
                    </th>
                    <th
                      className="cursor-pointer px-4 py-3 font-medium text-muted-foreground hover:text-foreground"
                      onClick={() => toggleSort('updatedAt')}
                    >
                      Updated{sortIndicator('updatedAt')}
                    </th>
                    <th className="px-4 py-3 font-medium text-muted-foreground">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((candidate) => (
                    <tr key={candidate.candidateId} className="border-b border-border hover:bg-slate-50/80">
                      <td className="px-4 py-3">
                        {candidate.profilePicturePath ? (
                          <img
                            src={pictureUrl(candidate.candidateId)}
                            alt=""
                            className="h-10 w-10 rounded-full object-cover bg-slate-100"
                            onError={(e) => {
                              ;(e.target as HTMLImageElement).style.display = 'none'
                            }}
                          />
                        ) : (
                          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-100 text-sm font-semibold text-brand-700">
                            {(candidate.fullName ?? '?')[0]?.toUpperCase()}
                          </div>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <div className="font-medium text-foreground">{candidate.fullName ?? '—'}</div>
                        {candidate.status && (
                          <Badge className={`mt-1 ${statusColor(candidate.status)}`}>
                            {candidate.status}
                          </Badge>
                        )}
                      </td>
                      <td className="px-4 py-3 text-muted-foreground">
                        {candidate.primaryEmail ? (
                          <span className="inline-flex items-center gap-1.5">
                            <span>{candidate.primaryEmail}</span>
                            <EmailValidationIcon status={candidate.primaryEmailValidationStatus} />
                          </span>
                        ) : (
                          '—'
                        )}
                      </td>
                      <td className="px-4 py-3 text-muted-foreground">{candidate.currentCompany ?? '—'}</td>
                      <td className="px-4 py-3 text-muted-foreground">
                        {candidate.yearsExperience != null ? `${candidate.yearsExperience} yrs` : '—'}
                      </td>
                      <td className="px-4 py-3">
                        <Badge className={confidenceColor(candidate.overallConfidence)}>
                          {formatConfidence(candidate.overallConfidence)}
                        </Badge>
                      </td>
                      <td className="px-4 py-3 text-muted-foreground">{formatDate(candidate.updatedAt)}</td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-1">
                          <Link to={`/candidates/${candidate.candidateId}`}>
                            <Button variant="ghost" size="sm">
                              <Eye className="h-4 w-4" />
                            </Button>
                          </Link>
                          <Button
                            variant="ghost"
                            size="sm"
                            disabled={reprocessMutation.isPending}
                            onClick={() => reprocessMutation.mutate(candidate.candidateId)}
                          >
                            <RefreshCw className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            disabled={deleteMutation.isPending}
                            onClick={() => {
                              if (confirm('Delete this candidate?')) {
                                deleteMutation.mutate(candidate.candidateId)
                              }
                            }}
                          >
                            <Trash2 className="h-4 w-4 text-red-600" />
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Page {data.page + 1} of {data.totalPages} · {data.totalElements} total
          </p>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              Previous
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={page >= data.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
