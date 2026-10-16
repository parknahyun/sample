import { Link } from 'react-router';

/** 존재하지 않거나 삭제된 상품(404) 안내. */
export function ProductNotFound() {
  return (
    <div className="notice" role="alert">
      <p>존재하지 않는 상품입니다</p>
      <p className="hint">삭제되었거나 주소가 잘못되었을 수 있습니다.</p>
      <Link to="/products">목록으로</Link>
    </div>
  );
}
