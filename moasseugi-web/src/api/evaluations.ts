import { apiRequest } from './http'
import { requireParticipantToken } from './participant-storage'

/** 좋음 1, 괜찮음 0, 별로 -1. */
export type EvaluationValue = -1 | 0 | 1

export interface EvaluationItem {
  ideaId: string
  content: string
  /** 아직 평가하지 않았으면 null. 미평가(null)와 괜찮음(0)은 다른 상태다. */
  myEvaluation: EvaluationValue | null
}

export interface Evaluation {
  ideaId: string
  value: EvaluationValue
}

/** 작성자를 가린 평가 대상과 내 평가를 조회합니다. 본인 아이디어는 제외됩니다. */
export function getEvaluationItems(sessionId: string): Promise<EvaluationItem[]> {
  return apiRequest<EvaluationItem[]>(
    `/sessions/${encodeURIComponent(sessionId)}/evaluation-items`,
    { participantToken: requireParticipantToken(sessionId) },
  )
}

/** 항목 하나의 평가를 저장하거나 바꿉니다. 응답의 value가 서버에 확정된 값입니다. */
export function putEvaluation(
  sessionId: string,
  ideaId: string,
  value: EvaluationValue,
): Promise<Evaluation> {
  return apiRequest<Evaluation>(`/sessions/${encodeURIComponent(sessionId)}/evaluations`, {
    method: 'PUT',
    body: { ideaId, value },
    participantToken: requireParticipantToken(sessionId),
  })
}
