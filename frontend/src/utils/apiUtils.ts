import axios from 'axios';

import type { ApiErrorResponse } from '@/types/api';

export const getApiErrorMessage = (error: unknown, fallbackMessage: string): string => {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    const message = error.response?.data?.message;

    // 서버 응답은 타입 선언과 달라질 수 있으므로 렌더링 전에 문자열인지 확인한다.
    if (typeof message === 'string' && message.trim()) {
      return message;
    }
  }

  return fallbackMessage;
};
