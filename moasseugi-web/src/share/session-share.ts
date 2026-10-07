import QRCode from 'qrcode'

/** 안건별 참가용 공유 주소를 현재 웹 origin에 만듭니다. */
export function createSessionShareUrl(sessionId: string): string {
  return new URL(`/s/${encodeURIComponent(sessionId)}`, window.location.origin).toString()
}

/** 참가 키가 포함되지 않은 공유 주소의 QR data URL을 만듭니다. */
export function createSessionQrDataUrl(sessionId: string): Promise<string> {
  return QRCode.toDataURL(createSessionShareUrl(sessionId))
}
