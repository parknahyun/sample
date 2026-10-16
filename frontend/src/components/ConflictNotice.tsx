interface ConflictNoticeProps {
  onReload: () => void;
}

/** 수정 저장이 409(다른 운영자가 먼저 저장)로 거절됐을 때의 안내. */
export function ConflictNotice({ onReload }: ConflictNoticeProps) {
  return (
    <div className="notice notice-warning" role="alert">
      <p>다른 사용자가 먼저 수정했습니다. 최신 내용을 다시 불러오세요</p>
      <button type="button" onClick={onReload}>
        다시 불러오기
      </button>
    </div>
  );
}
