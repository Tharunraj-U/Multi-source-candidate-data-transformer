export type CandidateStatus = 'DRAFT' | 'PROCESSING' | 'COMPLETED' | 'PARTIAL' | 'FAILED'
export type SourceType = 'RESUME' | 'LINKEDIN' | 'GITHUB' | 'RECRUITER_CSV' | 'ATS_JSON'
export type SourceStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
export type JobStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED'

export interface SourceSummary {
  sourceId: string
  sourceType: SourceType
  status: SourceStatus
}

export interface UploadResponse {
  candidateId: string
  status: CandidateStatus
  sources: SourceSummary[]
  createdAt: string
}

export interface ProcessResponse {
  jobId: string
  candidateId: string
  status: JobStatus
}

export interface JobStatusResponse {
  jobId: string
  candidateId: string
  status: JobStatus
  errorMessage?: string
}

export interface Skill {
  name: string
  canonical?: string
  confidence?: number
}

export interface Experience {
  company: string
  title?: string
  startDate?: string
  endDate?: string | null
  isCurrent?: boolean
  description?: string
  confidence?: number
}

export interface Education {
  institution: string
  degree?: string
  fieldOfStudy?: string
  startDate?: string
  endDate?: string
  confidence?: number
}

export interface Link {
  type: string
  url: string
  confidence?: number
}

export interface ProvenanceEntry {
  fieldPath: string
  value?: string
  sourceType: SourceType
  sourceId?: string
  capturedAt: string
  rawValue?: string
}

export interface RawSource {
  sourceId: string
  sourceType: SourceType
  status: SourceStatus
  originalFilename?: string
  sourceUrl?: string
  errorCode?: string
  errorMessage?: string
  ingestedAt?: string
}

export interface Candidate {
  candidateId: string
  fullName?: string
  headline?: string
  yearsExperience?: number
  overallConfidence?: number
  profilePicturePath?: string
  resumePath?: string
  emails?: string[]
  phones?: string[]
  location?: string
  skills?: Skill[]
  experience?: Experience[]
  education?: Education[]
  links?: Link[]
  provenance?: ProvenanceEntry[]
  confidence?: Record<string, number>
  status: CandidateStatus
  updatedAt?: string
  sources?: RawSource[]
}

export interface CandidateListItem {
  candidateId: string
  fullName?: string
  primaryEmail?: string
  currentCompany?: string
  yearsExperience?: number
  overallConfidence?: number
  profilePicturePath?: string
  status?: CandidateStatus
  updatedAt?: string
}

export interface CandidateListResponse {
  content: CandidateListItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CandidateListParams {
  search?: string
  minConfidence?: number
  company?: string
  status?: CandidateStatus
  page?: number
  size?: number
  sort?: string
}

export interface ApiError {
  title?: string
  detail?: string
  status?: number
  errorCode?: string
}
