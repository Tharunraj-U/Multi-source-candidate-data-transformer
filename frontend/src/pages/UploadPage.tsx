import { useMutation } from '@tanstack/react-query'
import { ArrowRight, CheckCircle2, Play, Upload as UploadIcon } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getJobStatus, processCandidate, uploadCandidate } from '../api/client'
import { Alert, Spinner } from '../components/ui/Feedback'
import { Button } from '../components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/Card'
import { Dropzone } from '../components/ui/Dropzone'
import { Input, Textarea } from '../components/ui/Input'
import type { UploadResponse } from '../types/candidate'

const DEFAULT_RUNTIME_CONFIG = `{
  "fields": [
    { "path": "candidateName", "from": "full_name" },
    { "path": "primaryEmail", "from": "emails[0]" }
  ],
  "includeConfidence": true,
  "includeProvenance": false,
  "missing": "omit"
}`

type PageState = 'idle' | 'uploading' | 'upload-success' | 'processing' | 'process-complete' | 'error'

export function UploadPage() {
  const navigate = useNavigate()
  const [resume, setResume] = useState<File | null>(null)
  const [recruiterCsv, setRecruiterCsv] = useState<File | null>(null)
  const [atsJson, setAtsJson] = useState<File | null>(null)
  const [gitHubUrl, setGitHubUrl] = useState('')
  const [runtimeConfig, setRuntimeConfig] = useState(DEFAULT_RUNTIME_CONFIG)
  const [configError, setConfigError] = useState<string>()
  const [pageState, setPageState] = useState<PageState>('idle')
  const [uploadResult, setUploadResult] = useState<UploadResponse | null>(null)
  const [jobId, setJobId] = useState<string | null>(null)
  const [errorMessage, setErrorMessage] = useState<string>()

  const uploadMutation = useMutation({
    mutationFn: uploadCandidate,
    onMutate: () => {
      setPageState('uploading')
      setErrorMessage(undefined)
    },
    onSuccess: (data) => {
      setUploadResult(data)
      setPageState('upload-success')
    },
    onError: (err: Error) => {
      setPageState('error')
      setErrorMessage(err.message)
    },
  })

  const processMutation = useMutation({
    mutationFn: processCandidate,
    onMutate: () => {
      setPageState('processing')
      setErrorMessage(undefined)
    },
    onSuccess: (data) => {
      setJobId(data.jobId)
    },
    onError: (err: Error) => {
      setPageState('error')
      setErrorMessage(err.message)
    },
  })

  const validateConfig = useCallback((): boolean => {
    if (!runtimeConfig.trim()) {
      setConfigError(undefined)
      return true
    }
    try {
      JSON.parse(runtimeConfig)
      setConfigError(undefined)
      return true
    } catch {
      setConfigError('Invalid JSON')
      return false
    }
  }, [runtimeConfig])

  const hasInput =
    resume || recruiterCsv || atsJson || gitHubUrl.trim()

  const handleUpload = () => {
    if (!hasInput) {
      setErrorMessage('Add at least one source before uploading.')
      setPageState('error')
      return
    }
    if (!validateConfig()) return

    const formData = new FormData()
    if (resume) formData.append('resume', resume)
    if (recruiterCsv) formData.append('recruiterCsv', recruiterCsv)
    if (atsJson) formData.append('atsJson', atsJson)
    if (gitHubUrl.trim()) formData.append('gitHubUrl', gitHubUrl.trim())
    if (runtimeConfig.trim()) formData.append('runtimeConfig', runtimeConfig.trim())
    uploadMutation.mutate(formData)
  }

  const handleProcess = () => {
    if (!uploadResult?.candidateId) return
    processMutation.mutate(uploadResult.candidateId)
  }

  useEffect(() => {
    if (!jobId || !uploadResult?.candidateId || pageState !== 'processing') return

    const poll = async () => {
      try {
        const status = await getJobStatus(uploadResult.candidateId, jobId)
        if (status.status === 'COMPLETED') {
          setPageState('process-complete')
        } else if (status.status === 'FAILED') {
          setPageState('error')
          setErrorMessage(status.errorMessage ?? 'Processing failed')
        }
      } catch (err) {
        setPageState('error')
        setErrorMessage(err instanceof Error ? err.message : 'Failed to poll job status')
      }
    }

    const interval = setInterval(poll, 2000)
    poll()
    return () => clearInterval(interval)
  }, [jobId, uploadResult?.candidateId, pageState])

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">Upload Candidate</h1>
        <p className="mt-1 text-muted-foreground">
          Upload resumes, URLs, CSV, or ATS JSON. Optionally attach a runtime projection config.
        </p>
      </div>

      {errorMessage && <Alert message={errorMessage} variant="error" />}
      {pageState === 'upload-success' && uploadResult && (
        <Alert
          message={`Upload successful — candidate ${uploadResult.candidateId.slice(0, 8)}… with ${uploadResult.sources.length} source(s).`}
          variant="success"
        />
      )}
      {pageState === 'process-complete' && (
        <Alert message="Processing complete. View the canonical profile below." variant="success" />
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Sources</CardTitle>
            <CardDescription>Provide one or more candidate data sources.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <Dropzone
              label="Resume (PDF / DOCX)"
              accept=".pdf,.doc,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
              file={resume}
              onFileChange={setResume}
            />
            <div className="grid gap-4 sm:grid-cols-2">
              <Dropzone
                label="Recruiter CSV"
                accept=".csv,text/csv"
                file={recruiterCsv}
                onFileChange={setRecruiterCsv}
              />
              <Dropzone
                label="ATS JSON"
                accept=".json,application/json"
                file={atsJson}
                onFileChange={setAtsJson}
              />
            </div>
            <Input
              label="GitHub Profile URL"
              placeholder="https://github.com/username"
              value={gitHubUrl}
              onChange={(e) => setGitHubUrl(e.target.value)}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Runtime Configuration</CardTitle>
            <CardDescription>JSON config for field projection and output shaping.</CardDescription>
          </CardHeader>
          <CardContent>
            <Textarea
              label="Runtime Config JSON"
              value={runtimeConfig}
              onChange={(e) => setRuntimeConfig(e.target.value)}
              onBlur={validateConfig}
              error={configError}
              className="min-h-[280px]"
            />
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardContent className="flex flex-wrap items-center gap-3 pt-6">
          <Button onClick={handleUpload} disabled={pageState === 'uploading' || !hasInput}>
            {pageState === 'uploading' ? <Spinner className="h-4 w-4" /> : <UploadIcon className="h-4 w-4" />}
            Upload
          </Button>
          <Button
            variant="secondary"
            onClick={handleProcess}
            disabled={!uploadResult || pageState === 'processing' || pageState === 'uploading'}
          >
            {pageState === 'processing' ? <Spinner className="h-4 w-4" /> : <Play className="h-4 w-4" />}
            Process Candidate
          </Button>
          <Button
            variant="outline"
            onClick={() => uploadResult && navigate(`/candidates/${uploadResult.candidateId}`)}
            disabled={!uploadResult}
          >
            View Result
            <ArrowRight className="h-4 w-4" />
          </Button>
          {uploadResult && (
            <span className="flex items-center gap-1.5 text-sm text-muted-foreground">
              <CheckCircle2 className="h-4 w-4 text-emerald-600" />
              ID: <code className="rounded bg-slate-100 px-1.5 py-0.5 text-xs">{uploadResult.candidateId}</code>
            </span>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
