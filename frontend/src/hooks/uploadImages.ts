import { uploadProductImage } from '../services/productApi';

/**
 * 선택한 이미지를 순서대로 한 장씩 올린다. 첫 업로드가 대표 이미지가 되므로 순서를 지킨다.
 * 한 장이라도 실패하면 거기서 멈추고 실패했다고 알린다(이미 올라간 이미지는 그대로 둔다).
 */
export async function uploadImagesSequentially(
  productId: number,
  files: File[],
): Promise<{ failed: boolean }> {
  for (const file of files) {
    try {
      await uploadProductImage(productId, file);
    } catch {
      return { failed: true };
    }
  }
  return { failed: false };
}
