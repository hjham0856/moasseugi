import { apiRequest } from './http'
import {
  requireParticipantToken,
  storeParticipant,
  type ParticipantSummary,
  type StoredParticipant,
} from './participant-storage'

export type SessionStatus = 'WRITING' | 'EVALUATING' | 'RESULT'

export interface SessionDetail {
  id: string
  title: string
  description: string | null
  status: SessionStatus
  selectedIdeaId: string | null
}

export interface CreateSessionRequest {
  title: string
  description?: string | null
  nickname: string
}

export interface CreateSessionResponse extends StoredParticipant {
  session: SessionDetail
}

export type JoinSessionResponse = StoredParticipant

/** 안건을 만들고 진행자 정보를 안건별로 보관합니다. */
export async function createSession(
  request: CreateSessionRequest,
): Promise<CreateSessionResponse> {
  const response = await apiRequest<CreateSessionResponse>('/sessions', {
    method: 'POST',
    body: request,
  })

  storeParticipant(response.session.id, response)
  return response
}

/** 공유 주소에서 참가 키 없이 안건의 공개 정보를 조회합니다. */
export function getSession(sessionId: string): Promise<SessionDetail> {
  return apiRequest<SessionDetail>(`/sessions/${encodeURIComponent(sessionId)}`)
}

/** 닉네임으로 참가하고 발급된 키를 안건별로 보관합니다. */
export async function joinSession(
  sessionId: string,
  nickname: string,
): Promise<JoinSessionResponse> {
  const response = await apiRequest<JoinSessionResponse>(
    `/sessions/${encodeURIComponent(sessionId)}/participants`,
    { method: 'POST', body: { nickname } },
  )

  storeParticipant(sessionId, response)
  return response
}

/** 진행자 화면에서 키가 필요한 참가자 목록을 조회합니다. */
export function getSessionParticipants(sessionId: string): Promise<ParticipantSummary[]> {
  return apiRequest<ParticipantSummary[]>(
    `/sessions/${encodeURIComponent(sessionId)}/participants`,
    { participantToken: requireParticipantToken(sessionId) },
  )
}

/**
 * 진행자가 작성을 마치고 평가를 시작합니다. 되돌릴 수 없습니다.
 * 아이디어가 하나도 없으면 409와 안내 문구로 거부됩니다.
 */
export function closeWriting(sessionId: string): Promise<SessionDetail> {
  return apiRequest<SessionDetail>(`/sessions/${encodeURIComponent(sessionId)}/close-writing`, {
    method: 'POST',
    participantToken: requireParticipantToken(sessionId),
  })
}

/** 진행자가 평가를 마치고 결과 단계로 넘어갑니다. 되돌릴 수 없습니다. */
export function closeEvaluation(sessionId: string): Promise<SessionDetail> {
  return apiRequest<SessionDetail>(`/sessions/${encodeURIComponent(sessionId)}/close-evaluation`, {
    method: 'POST',
    participantToken: requireParticipantToken(sessionId),
  })
}
