import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Download, ExternalLink } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getCandidate, pictureUrl, resumeUrl } from '../api/client'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { Card, CardContent, CardHeader } from '../components/ui/Card'
import { Alert, EmptyState, Spinner } from '../components/ui/Feedback'
import { Tabs } from '../components/ui/Tabs'
import type { ProvenanceEntry } from '../types/candidate'
import { confidenceColor, formatConfidence, formatDate, statusColor } from '../lib/utils'

const DETAIL_TABS = [
  { id: 'personal', label: 'Personal' },
  { id: 'skills', label: 'Skills' },
  { id: 'experience', label: 'Experience' },
  { id: 'education', label: 'Education' },
  { id: 'links', label: 'Social Links' },
  { id: 'resume', label: 'Resume' },
  { id: 'confidence', label: 'Confidence' },
  { id: 'provenance', label: 'Provenance' },
  { id: 'sources', label: 'Raw Sources' },
]

function ProvenanceTree({ entries }: { entries: ProvenanceEntry[] }) {
  const grouped = entries.reduce<Record<string, ProvenanceEntry[]>>((acc, entry) => {
    const key = entry.fieldPath
    if (!acc[key]) acc[key] = []
    acc[key].push(entry)
    return acc
  }, {})

  const fields = Object.keys(grouped).sort()

  if (!fields.length) {
    return <EmptyState title="No provenance data" />
  }

  return (
    <div className="space-y-3">
      {fields.map((field) => (
        <details key={field} className="rounded-lg border border-border bg-slate-50">
          <summary className="cursor-pointer px-4 py-3 text-sm font-medium text-foreground">
            {field}
          </summary>
          <div className="space-y-2 border-t border-border px-4 py-3">
            {grouped[field].map((entry, i) => (
              <div key={i} className="rounded-md bg-white p-3 text-sm">
                <div className="flex flex-wrap gap-2">
                  <Badge className="bg-slate-100 text-slate-700">{entry.sourceType}</Badge>
                  <span className="text-muted-foreground">{formatDate(entry.capturedAt)}</span>
                </div>
                <p className="mt-2 font-medium">{entry.value ?? '—'}</p>
                {entry.rawValue && entry.rawValue !== entry.value && (
                  <p className="mt-1 text-xs text-muted-foreground">Raw: {entry.rawValue}</p>
                )}
              </div>
            ))}
          </div>
        </details>
      ))}
    </div>
  )
}

export function CandidateDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [activeTab, setActiveTab] = useState('personal')

  const { data: candidate, isLoading, error } = useQuery({
    queryKey: ['candidate', id],
    queryFn: () => getCandidate(id!),
    enabled: !!id,
  })

  if (isLoading) {
    return (
      <div className="flex justify-center py-24">
        <Spinner />
      </div>
    )
  }

  if (error || !candidate) {
    return (
      <div className="space-y-4">
        <Link to="/candidates" className="inline-flex items-center gap-1 text-sm text-brand-600 hover:underline">
          <ArrowLeft className="h-4 w-4" />
          Back to list
        </Link>
        <Alert message={(error as Error)?.message ?? 'Candidate not found'} variant="error" />
      </div>
    )
  }

  const confidenceEntries = candidate.confidence
    ? Object.entries(candidate.confidence).sort(([a], [b]) => a.localeCompare(b))
    : []

  return (
    <div className="space-y-6">
      <Link to="/candidates" className="inline-flex items-center gap-1 text-sm text-brand-600 hover:underline">
        <ArrowLeft className="h-4 w-4" />
        Back to candidates
      </Link>

      <Card>
        <CardContent className="flex flex-col gap-6 p-6 sm:flex-row sm:items-center">
          {candidate.profilePicturePath ? (
            <img
              src={pictureUrl(candidate.candidateId)}
              alt={candidate.fullName ?? 'Profile'}
              className="h-24 w-24 rounded-full object-cover bg-slate-100"
            />
          ) : (
            <div className="flex h-24 w-24 items-center justify-center rounded-full bg-brand-100 text-3xl font-bold text-brand-700">
              {(candidate.fullName ?? '?')[0]?.toUpperCase()}
            </div>
          )}
          <div className="flex-1">
            <div className="flex flex-wrap items-center gap-3">
              <h1 className="text-2xl font-bold text-foreground">{candidate.fullName ?? 'Unnamed Candidate'}</h1>
              <Badge className={statusColor(candidate.status)}>{candidate.status}</Badge>
              <Badge className={confidenceColor(candidate.overallConfidence)}>
                {formatConfidence(candidate.overallConfidence)} confidence
              </Badge>
            </div>
            {candidate.headline && (
              <p className="mt-1 text-muted-foreground">{candidate.headline}</p>
            )}
            <p className="mt-2 text-sm text-muted-foreground">
              Updated {formatDate(candidate.updatedAt)}
            </p>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="pb-0">
          <Tabs tabs={DETAIL_TABS} activeTab={activeTab} onTabChange={setActiveTab} />
        </CardHeader>
        <CardContent className="pt-6">
          {activeTab === 'personal' && (
            <dl className="grid gap-4 sm:grid-cols-2">
              <div>
                <dt className="text-xs font-medium uppercase text-muted-foreground">Full Name</dt>
                <dd className="mt-1 text-foreground">{candidate.fullName ?? '—'}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase text-muted-foreground">Location</dt>
                <dd className="mt-1 text-foreground">{candidate.location ?? '—'}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase text-muted-foreground">Years Experience</dt>
                <dd className="mt-1 text-foreground">
                  {candidate.yearsExperience != null ? `${candidate.yearsExperience} years` : '—'}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase text-muted-foreground">Emails</dt>
                <dd className="mt-1 text-foreground">
                  {candidate.emails?.length ? (
                    <ul className="space-y-1">
                      {candidate.emails.map((email) => (
                        <li key={email.address}>{email.address}</li>
                      ))}
                    </ul>
                  ) : (
                    '—'
                  )}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase text-muted-foreground">Phones</dt>
                <dd className="mt-1 text-foreground">
                  {candidate.phones?.length ? candidate.phones.join(', ') : '—'}
                </dd>
              </div>
            </dl>
          )}

          {activeTab === 'skills' && (
            <div className="flex flex-wrap gap-2">
              {candidate.skills?.length ? (
                candidate.skills.map((skill) => (
                  <div
                    key={skill.canonical ?? skill.name}
                    className="flex items-center gap-2 rounded-full border border-border bg-slate-50 px-3 py-1.5"
                  >
                    <span className="text-sm font-medium">{skill.canonical ?? skill.name}</span>
                    {skill.confidence != null && (
                      <Badge className={confidenceColor(skill.confidence)}>
                        {formatConfidence(skill.confidence)}
                      </Badge>
                    )}
                  </div>
                ))
              ) : (
                <EmptyState title="No skills recorded" />
              )}
            </div>
          )}

          {activeTab === 'experience' && (
            <div className="space-y-4">
              {candidate.experience?.length ? (
                candidate.experience.map((exp, i) => (
                  <div key={i} className="relative border-l-2 border-brand-200 pl-6">
                    <div className="absolute -left-[5px] top-1.5 h-2 w-2 rounded-full bg-brand-600" />
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <div>
                        <h3 className="font-semibold text-foreground">{exp.title ?? 'Role'}</h3>
                        <p className="text-brand-700">{exp.company}</p>
                      </div>
                      {exp.confidence != null && (
                        <Badge className={confidenceColor(exp.confidence)}>
                          {formatConfidence(exp.confidence)}
                        </Badge>
                      )}
                    </div>
                    <p className="mt-1 text-sm text-muted-foreground">
                      {exp.startDate ?? '?'} — {exp.isCurrent ? 'Present' : (exp.endDate ?? '?')}
                    </p>
                    {exp.description && (
                      <p className="mt-2 text-sm text-foreground">{exp.description}</p>
                    )}
                  </div>
                ))
              ) : (
                <EmptyState title="No experience recorded" />
              )}
            </div>
          )}

          {activeTab === 'education' && (
            <div className="space-y-4">
              {candidate.education?.length ? (
                candidate.education.map((edu, i) => (
                  <div key={i} className="rounded-lg border border-border p-4">
                    <h3 className="font-semibold text-foreground">{edu.institution}</h3>
                    <p className="text-sm text-muted-foreground">
                      {[edu.degree, edu.fieldOfStudy].filter(Boolean).join(' · ') || '—'}
                    </p>
                    <p className="mt-1 text-sm text-muted-foreground">
                      {edu.startDate ?? '?'} — {edu.endDate ?? '?'}
                    </p>
                  </div>
                ))
              ) : (
                <EmptyState title="No education recorded" />
              )}
            </div>
          )}

          {activeTab === 'links' && (
            <div className="space-y-3">
              {candidate.links?.length ? (
                candidate.links.map((link, i) => (
                  <a
                    key={i}
                    href={link.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center justify-between rounded-lg border border-border px-4 py-3 hover:bg-slate-50"
                  >
                    <div>
                      <p className="font-medium text-foreground">{link.type}</p>
                      <p className="text-sm text-muted-foreground">{link.url}</p>
                    </div>
                    <ExternalLink className="h-4 w-4 text-muted-foreground" />
                  </a>
                ))
              ) : (
                <EmptyState title="No social links" />
              )}
            </div>
          )}

          {activeTab === 'resume' && (
            <div className="space-y-4">
              {candidate.resumePath ? (
                <>
                  <a
                    href={resumeUrl(candidate.candidateId)}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex"
                  >
                    <Button variant="outline">
                      <Download className="h-4 w-4" />
                      Download Resume
                    </Button>
                  </a>
                  <iframe
                    src={resumeUrl(candidate.candidateId)}
                    title="Resume preview"
                    className="h-[600px] w-full rounded-lg border border-border bg-white"
                  />
                </>
              ) : (
                <EmptyState title="No resume on file" />
              )}
            </div>
          )}

          {activeTab === 'confidence' && (
            <div className="overflow-x-auto">
              {confidenceEntries.length ? (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border text-left">
                      <th className="px-3 py-2 font-medium text-muted-foreground">Field</th>
                      <th className="px-3 py-2 font-medium text-muted-foreground">Score</th>
                    </tr>
                  </thead>
                  <tbody>
                    {confidenceEntries.map(([field, score]) => (
                      <tr key={field} className="border-b border-border">
                        <td className="px-3 py-2 font-mono text-xs">{field}</td>
                        <td className="px-3 py-2">
                          <Badge className={confidenceColor(score)}>{formatConfidence(score)}</Badge>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <EmptyState title="No confidence scores" />
              )}
            </div>
          )}

          {activeTab === 'provenance' && (
            <ProvenanceTree entries={candidate.provenance ?? []} />
          )}

          {activeTab === 'sources' && (
            <div className="space-y-3">
              {candidate.sources?.length ? (
                candidate.sources.map((source) => (
                  <div key={source.sourceId} className="rounded-lg border border-border p-4">
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge className="bg-slate-100 text-slate-700">{source.sourceType}</Badge>
                      <Badge className={statusColor(source.status)}>{source.status}</Badge>
                    </div>
                    {source.originalFilename && (
                      <p className="mt-2 text-sm text-foreground">{source.originalFilename}</p>
                    )}
                    {source.sourceUrl && (
                      <p className="mt-1 text-sm text-muted-foreground">{source.sourceUrl}</p>
                    )}
                    {source.errorMessage && (
                      <p className="mt-2 text-sm text-red-600">{source.errorMessage}</p>
                    )}
                  </div>
                ))
              ) : (
                <EmptyState title="No raw sources" description="Source metadata appears after upload." />
              )}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
