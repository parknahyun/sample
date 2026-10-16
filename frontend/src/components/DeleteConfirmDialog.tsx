interface DeleteConfirmDialogProps {
  open: boolean;
  productName: string;
  pending?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

/** 상품 삭제 전 확인 대화상자. 확인 전에는 삭제 요청을 보내지 않는다. */
export function DeleteConfirmDialog({
  open,
  productName,
  pending,
  onConfirm,
  onCancel,
}: DeleteConfirmDialogProps) {
  if (!open) return null;

  return (
    <div className="dialog-backdrop">
      <div className="dialog" role="dialog" aria-modal="true" aria-labelledby="delete-dialog-title">
        <h2 id="delete-dialog-title">상품 삭제</h2>
        <p>
          &lsquo;{productName}&rsquo; 상품을 삭제하시겠습니까? 삭제한 상품은 목록과 고객 화면에서
          사라집니다.
        </p>
        <div className="actions">
          <button type="button" onClick={onCancel} disabled={pending}>
            취소
          </button>
          <button type="button" className="danger" onClick={onConfirm} disabled={pending}>
            삭제 확인
          </button>
        </div>
      </div>
    </div>
  );
}
