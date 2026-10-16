import { Link, Navigate, Route, Routes } from 'react-router';
import { ProductCreatePage } from './pages/ProductCreatePage';
import { ProductDetailPage } from './pages/ProductDetailPage';
import { ProductEditPage } from './pages/ProductEditPage';
import { ProductListPage } from './pages/ProductListPage';

/** 운영자 화면 라우팅. */
export function App() {
  return (
    <>
      <header className="app-header">
        <Link to="/products">상품 관리</Link>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/products" replace />} />
          <Route path="/products" element={<ProductListPage />} />
          <Route path="/products/new" element={<ProductCreatePage />} />
          <Route path="/products/:productId" element={<ProductDetailPage />} />
          <Route path="/products/:productId/edit" element={<ProductEditPage />} />
          <Route
            path="*"
            element={
              <p>
                페이지를 찾을 수 없습니다. <Link to="/products">목록으로</Link>
              </p>
            }
          />
        </Routes>
      </main>
    </>
  );
}
