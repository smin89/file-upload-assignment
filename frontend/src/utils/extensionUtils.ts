export const MAX_EXTENSION_LENGTH = 20;

export const EXTENSION_LENGTH_ERROR = `확장자는 최대 ${MAX_EXTENSION_LENGTH}자까지 입력할 수 있습니다.`;

// 서버와 같은 순서로 공백·대소문자·선행 점 하나를 정리한 뒤 길이를 검사한다.
export const normalizeExtension = (value: string): string =>
  value.trim().toLowerCase().replace(/^\./, '');
