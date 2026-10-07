const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
).replace(/\/+$/, '')

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT'
  body?: unknown
  participantToken?: string
}

/** HTTP 오류의 상태 코드와 서버가 반환한 안내 문구를 보관합니다. */
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

/** API 요청을 보내고 실패 응답을 상태와 message로 전달합니다. */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers()

  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }

  if (options.participantToken) {
    headers.set('X-Participant-Token', options.participantToken)
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: options.method ?? 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  })
  const responseText = await response.text()
  let payload: unknown

  if (responseText) {
    try {
      payload = JSON.parse(responseText)
    } catch {
      if (response.ok) {
        throw new Error('서버 응답을 읽을 수 없습니다.')
      }
    }
  }

  if (!response.ok) {
    const message =
      typeof payload === 'object' && payload !== null && 'message' in payload &&
      typeof payload.message === 'string'
        ? payload.message
        : `요청에 실패했습니다. (HTTP ${response.status})`

    throw new ApiError(response.status, message)
  }

  return payload as T
}
