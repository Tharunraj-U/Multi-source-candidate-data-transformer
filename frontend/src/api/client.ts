import type {
  ApiError,
  Candidate,
  CandidateListParams,
  CandidateListResponse,
  JobStatusResponse,
  ProcessResponse,
  UploadResponse,
} from '../types/candidate'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let error: ApiError = { status: response.status, title: response.statusText }
    try {
      error = await response.json()
    } catch {
      // ignore parse errors
    }
    throw new Error(error.detail ?? error.title ?? `Request failed (${response.status})`)
  }
  if (response.status === 204) return undefined as T
  return response.json()
}

export async function uploadCandidate(formData: FormData): Promise<UploadResponse> {
  const response = await fetch(`${API_BASE}/api/v1/candidate/upload`, {
    method: 'POST',
    body: formData,
  })
  return handleResponse<UploadResponse>(response)
}

export async function processCandidate(candidateId: string): Promise<ProcessResponse> {
  const response = await fetch(`${API_BASE}/api/v1/candidate/process`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ candidateId }),
  })
  return handleResponse<ProcessResponse>(response)
}

export async function getJobStatus(
  candidateId: string,
  jobId: string,
): Promise<JobStatusResponse> {
  const response = await fetch(
    `${API_BASE}/api/v1/candidate/${candidateId}/job/${jobId}`,
  )
  return handleResponse<JobStatusResponse>(response)
}

export async function getCandidate(
  id: string,
  params?: { projection?: boolean; includeProvenance?: boolean; includeConfidence?: boolean },
): Promise<Candidate> {
  const search = new URLSearchParams()
  if (params?.projection) search.set('projection', 'true')
  if (params?.includeProvenance === false) search.set('includeProvenance', 'false')
  if (params?.includeConfidence === false) search.set('includeConfidence', 'false')
  const qs = search.toString()
  const response = await fetch(
    `${API_BASE}/api/v1/candidate/${id}${qs ? `?${qs}` : ''}`,
  )
  return handleResponse<Candidate>(response)
}

export async function listCandidates(
  params: CandidateListParams = {},
): Promise<CandidateListResponse> {
  const search = new URLSearchParams()
  if (params.search) search.set('search', params.search)
  if (params.minConfidence != null) search.set('minConfidence', String(params.minConfidence))
  if (params.company) search.set('company', params.company)
  if (params.status) search.set('status', params.status)
  if (params.page != null) search.set('page', String(params.page))
  if (params.size != null) search.set('size', String(params.size))
  if (params.sort) search.set('sort', params.sort)
  const qs = search.toString()
  const response = await fetch(`${API_BASE}/api/v1/candidate${qs ? `?${qs}` : ''}`)
  return handleResponse<CandidateListResponse>(response)
}

export async function deleteCandidate(id: string): Promise<void> {
  const response = await fetch(`${API_BASE}/api/v1/candidate/${id}`, { method: 'DELETE' })
  return handleResponse<void>(response)
}

export async function reprocessCandidate(id: string): Promise<ProcessResponse> {
  const response = await fetch(`${API_BASE}/api/v1/candidate/${id}/reprocess`, {
    method: 'POST',
  })
  return handleResponse<ProcessResponse>(response)
}

export function resumeUrl(candidateId: string): string {
  return `${API_BASE}/api/v1/candidate/${candidateId}/resume`
}

export function pictureUrl(candidateId: string): string {
  return `${API_BASE}/api/v1/candidate/${candidateId}/picture`
}
