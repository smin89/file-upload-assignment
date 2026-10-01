import axios from 'axios';

import type { ApiErrorResponse } from '../types/api';

export const getApiErrorMessage = (
  error: unknown,
  fallbackMessage: string,
): string => {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    const message = error.response?.data?.message;

    if (message) {
      return message;
    }
  }

  return fallbackMessage;
};