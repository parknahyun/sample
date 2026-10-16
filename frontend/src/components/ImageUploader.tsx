import { useState } from 'react';

export const MAX_IMAGES = 10;
export const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
const ALLOWED_TYPES = ['image/jpeg', 'image/png'];

/** 이미지 한 장을 사전 검증한다. 문제가 없으면 null, 있으면 안내 문구를 돌려준다. */
export function validateImageFile(file: File): string | null {
  if (!ALLOWED_TYPES.includes(file.type)) {
    return `${file.name}: JPG 또는 PNG 파일만 올릴 수 있습니다`;
  }
  if (file.size > MAX_IMAGE_BYTES) {
    return `${file.name}: 파일 크기는 5MB 이하여야 합니다`;
  }
  return null;
}

interface ImageUploaderProps {
  /** 선택된 파일. 첫 번째 파일이 대표 이미지가 된다(첫 업로드가 자동으로 대표로 지정되므로). */
  files: File[];
  onChange: (files: File[]) => void;
  /** 추가로 올릴 수 있는 장수(기존 이미지가 있으면 10에서 뺀 값). 기본 10. */
  remaining?: number;
  disabled?: boolean;
}

/** 이미지 선택(JPG·PNG, 최대 10장, 5MB 사전 검증)과 대표 지정 UI. */
export function ImageUploader({
  files,
  onChange,
  remaining = MAX_IMAGES,
  disabled,
}: ImageUploaderProps) {
  const [errors, setErrors] = useState<string[]>([]);
  function handleSelect(selected: FileList | null) {
    const incoming = Array.from(selected ?? []);
    const problems: string[] = [];
    const accepted: File[] = [];
    for (const file of incoming) {
      const problem = validateImageFile(file);
      if (problem) problems.push(problem);
      else if (files.length + accepted.length >= remaining) {
        problems.push(`${file.name}: 이미지는 상품당 최대 ${MAX_IMAGES}장까지 올릴 수 있습니다`);
      } else accepted.push(file);
    }
    setErrors(problems);
    if (accepted.length > 0) onChange([...files, ...accepted]);
  }

  function makePrimary(index: number) {
    onChange([files[index], ...files.filter((_, i) => i !== index)]);
  }

  return (
    <fieldset className="image-uploader" disabled={disabled}>
      <legend>이미지 (선택, 최대 {MAX_IMAGES}장)</legend>
      <input
        type="file"
        aria-label="이미지 선택"
        accept="image/jpeg,image/png"
        multiple
        onChange={(event) => {
          handleSelect(event.target.files);
          event.target.value = '';
        }}
      />
      {errors.length > 0 && (
        <ul className="field-error" role="alert">
          {errors.map((error) => (
            <li key={error}>{error}</li>
          ))}
        </ul>
      )}
      <ul className="image-list">
        {files.map((file, index) => (
          <li key={`${file.name}-${index}`}>
            <span>
              {file.name} ({Math.ceil(file.size / 1024)}KB)
            </span>
            {index === 0 ? (
              <span className="badge">대표</span>
            ) : (
              <button
                type="button"
                aria-label={`${file.name} 대표로 지정`}
                onClick={() => makePrimary(index)}
              >
                대표로 지정
              </button>
            )}
            <button
              type="button"
              aria-label={`${file.name} 제거`}
              onClick={() => onChange(files.filter((_, i) => i !== index))}
            >
              제거
            </button>
          </li>
        ))}
      </ul>
    </fieldset>
  );
}
