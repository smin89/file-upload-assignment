import type { ExtensionSetting } from '../types/extension';
import type { UploadSetting } from '../types/setting';
import type { FileValidationResult } from '../types/file';

/**
 * 파일 크기를 읽기 쉬운 형태로 변환
 * ex) 1024 -> 1.00 KB
 */
export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) {
    return '0 B';
  }

  const units = ['B', 'KB', 'MB', 'GB'];
  const unitIndex = Math.floor(Math.log(bytes) / Math.log(1024));
  const size = bytes / Math.pow(1024, unitIndex);

  return `${size.toFixed(unitIndex === 0 ? 0 : 2)} ${units[unitIndex]}`;
};

/**
 * 파일명에 포함된 모든 확장자를 추출
 *
 * test.pdf      -> ['pdf']
 * test.exe.pdf  -> ['exe', 'pdf']
 * archive.tar.gz -> ['tar', 'gz']
 * .env          -> []
 * .env.exe      -> ['exe']
 */
export const getFileExtensions = (
  fileName: string,
): string[] => {
  const lowerName = fileName.toLowerCase();
  const parts = lowerName.split('.');

  // .env, .gitignore처럼 "."으로 시작하는 파일 처리
  if (fileName.startsWith('.')) {
    return parts.slice(2);
  }

  return parts.slice(1);
};

/**
 * 파일 업로드 정책 검증
 */
export const validateFile = (
  file: File,
  setting: UploadSetting,
  extensionSetting: ExtensionSetting,
): FileValidationResult => {
  if (file.size === 0) {
    return {
      valid: false,
      message: '빈 파일은 업로드할 수 없습니다.',
    };
  }

  // 1. 파일 크기 검증
  if (file.size > setting.maxFileSize) {
    return {
      valid: false,
      message: `최대 파일 크기 ${formatFileSize(setting.maxFileSize)}를 초과했습니다.`,
    };
  }

  // 2. 파일에 포함된 모든 확장자 추출
  const fileExtensions = getFileExtensions(file.name);

  // 3. 활성화된 고정 제한 확장자
  const blockedFixedExtensions =
    extensionSetting.fixedExtensions
      .filter((item) => item.enabled)
      .map((item) => item.extension.toLowerCase());

  // 4. 사용자 추가 제한 확장자
  const blockedCustomExtensions =
    extensionSetting.customExtensions
      .map((item) => item.extension.toLowerCase());

  // 5. 전체 제한 확장자
  const blockedExtensions = [
    ...blockedFixedExtensions,
    ...blockedCustomExtensions,
  ];

  // 6. 파일 확장자 중 제한된 확장자가 있는지 검사
  const blockedExtension = fileExtensions.find((extension) =>
    blockedExtensions.includes(extension),
  );

  if (blockedExtension) {
    return {
      valid: false,
      message: `.${blockedExtension} 확장자는 업로드할 수 없습니다.`,
    };
  }

  return {
    valid: true,
  };
};

/**
 * 동일 파일 여부 확인
 */
export const isSameFile = (
  file1: File,
  file2: File,
): boolean => {
  return (
    file1.name === file2.name &&
    file1.size === file2.size &&
    file1.lastModified === file2.lastModified
  );
};