import { AlertCircle, BadgeCheck } from 'lucide-react'
import type { CandidateEmail } from '../types/candidate'

type EmailValidationStatus = CandidateEmail['validationStatus']

interface EmailValidationIconProps {
  status?: EmailValidationStatus | string | null
  className?: string
}

export function EmailValidationIcon({ status, className = 'h-4 w-4' }: EmailValidationIconProps) {
  if (status === 'valid') {
    return (
      <span title="Valid email format">
        <BadgeCheck className={`${className} text-brand-600`} aria-label="Valid email" />
      </span>
    )
  }

  if (status === 'invalid') {
    return (
      <span title="Invalid email format">
        <AlertCircle className={`${className} text-red-500`} aria-label="Invalid email" />
      </span>
    )
  }

  return null
}

interface EmailWithValidationProps {
  email: CandidateEmail
}

export function EmailWithValidation({ email }: EmailWithValidationProps) {
  return (
    <span className="inline-flex items-center gap-1.5">
      <span>{email.address}</span>
      <EmailValidationIcon status={email.validationStatus} />
    </span>
  )
}
