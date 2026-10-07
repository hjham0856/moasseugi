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
