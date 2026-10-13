import { apiRequest } from './http'
import { requireParticipantToken } from './participant-storage'

export interface Idea {
  id: string
  content: string
}

/** 내 아이디어를 조회합니다. 아직 저장하지 않았으면 null입니다. 다른 참가자의 것은 오지 않습니다. */
export function getMyIdea(sessionId: string): Promise<Idea | null> {
  return apiRequest<Idea | null>(`/sessions/${encodeURIComponent(sessionId)}/ideas/me`, {
    participantToken: requireParticipantToken(sessionId),
  })
}

/** 내 아이디어를 저장하거나 수정합니다. 참가자당 하나이므로 다시 호출해도 늘어나지 않습니다. */
export function putIdea(sessionId: string, content: string): Promise<Idea> {
  return apiRequest<Idea>(`/sessions/${encodeURIComponent(sessionId)}/ideas`, {
    method: 'PUT',
    body: { content },
    participantToken: requireParticipantToken(sessionId),
  })
}
