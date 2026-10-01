import api from './axios';
import type { ApiResponse } from '../types/api';

export const uploadFiles = async (
  files: File[],
): Promise<ApiResponse<unknown>> => {
  const formData = new FormData();

  files.forEach((file) => {
    formData.append('files', file);
  });

  const response = await api.post<ApiResponse<unknown>>(
    '/files',
    formData,
  );

  return response.data;
};