export interface ParticipantSummary {
  id: string
  nickname: string
  isHost: boolean
}

export interface StoredParticipant {
  participant: ParticipantSummary
  participantToken: string
}

function storageKey(sessionId: string): string {
  return `moasseugi.participant.${sessionId}`
}

function isParticipantSummary(value: unknown): value is ParticipantSummary {
  return (
    typeof value === 'object' &&
    value !== null &&
    'id' in value &&
    typeof value.id === 'string' &&
    'nickname' in value &&
    typeof value.nickname === 'string' &&
    'isHost' in value &&
    typeof value.isHost === 'boolean'
  )
}

function isStoredParticipant(value: unknown): value is StoredParticipant {
  return (
    typeof value === 'object' &&
    value !== null &&
    'participant' in value &&
    isParticipantSummary(value.participant) &&
    'participantToken' in value &&
    typeof value.participantToken === 'string'
  )
}

/** 참가자 정보와 안건별 키를 브라우저 저장소에 보관합니다. */
export function storeParticipant(sessionId: string, participant: StoredParticipant): void {
  if (typeof window === 'undefined') {
    return
  }

  const storedParticipant: StoredParticipant = {
    participant: participant.participant,
    participantToken: participant.participantToken,
  }

  window.localStorage.setItem(storageKey(sessionId), JSON.stringify(storedParticipant))
}

/**
 * 참가자 전용 요청에 쓸 키를 읽습니다.
 * 키로 신원을 찾는 곳을 한 군데로 모으기 위해 모든 참가자 API가 이 함수를 거치며,
 * 키가 없으면 요청을 보내지 않고 오류를 던집니다.
 */
export function requireParticipantToken(sessionId: string): string {
  const participant = getStoredParticipant(sessionId)

  if (!participant) {
    throw new Error('이 안건의 참가 정보가 없습니다.')
  }

  return participant.participantToken
}

/** 재접속 시 해당 안건의 참가자 정보와 키를 읽습니다. */
export function getStoredParticipant(sessionId: string): StoredParticipant | null {
  if (typeof window === 'undefined') {
    return null
  }

  const value = window.localStorage.getItem(storageKey(sessionId))

  if (!value) {
    return null
  }

  try {
    const participant = JSON.parse(value) as unknown
    return isStoredParticipant(participant) ? participant : null
  } catch {
    return null
  }
}
