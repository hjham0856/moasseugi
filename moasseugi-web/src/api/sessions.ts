import { apiRequest } from './http'
import {
  getStoredParticipant,
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
  const participant = getStoredParticipant(sessionId)

  if (!participant) {
    throw new Error('이 안건의 참가 정보가 없습니다.')
  }

  return apiRequest<ParticipantSummary[]>(
    `/sessions/${encodeURIComponent(sessionId)}/participants`,
    { participantToken: participant.participantToken },
  )
}
