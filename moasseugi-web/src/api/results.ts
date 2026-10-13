import { apiRequest } from './http'
import { requireParticipantToken } from './participant-storage'

/** 평가 분포. 점수나 순위로 합치지 않고 선택지별 인원을 그대로 전달합니다. */
export interface EvaluationDistribution {
  positive: number
  neutral: number
  negative: number
  total: number
}

export interface IdeaResult {
  ideaId: string
  content: string
  /** 채택된 아이디어에만 값이 있고 나머지는 null입니다. */
  authorNickname: string | null
  distribution: EvaluationDistribution
  selected: boolean
}

export interface ResultResponse {
  sessionId: string
  status: 'RESULT'
  selectedIdeaId: string | null
  /** 생성순입니다. */
  ideas: IdeaResult[]
}

/** 공유 링크로 결과를 조회합니다. 참가 키가 필요 없으며 RESULT 전에는 409입니다. */
export function getResults(sessionId: string): Promise<ResultResponse> {
  return apiRequest<ResultResponse>(`/sessions/${encodeURIComponent(sessionId)}/results`)
}

/** 진행자가 아이디어 하나를 채택합니다. 한 번 채택하면 바꿀 수 없어 이미 채택됐으면 409입니다. */
export function selectIdea(sessionId: string, ideaId: string): Promise<ResultResponse> {
  return apiRequest<ResultResponse>(`/sessions/${encodeURIComponent(sessionId)}/select-idea`, {
    method: 'POST',
    body: { ideaId },
    participantToken: requireParticipantToken(sessionId),
  })
}
