/** 원화 금액 표시. 예: 19000 -> `19,000원` */
export function formatPrice(price: number): string {
  return `${price.toLocaleString('ko-KR')}원`;
}

/** ISO 일시를 `2026. 10. 7.` 형태의 날짜로 표시한다. */
export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('ko-KR');
}
