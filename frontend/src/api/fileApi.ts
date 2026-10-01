import api from './axios';

import type { ApiResponse } from '../types/api';
import type { UploadResult } from '../types/file';

export const uploadFiles = async (
  files: File[],
): Promise<ApiResponse<UploadResult>> => {
  const formData = new FormData();

  files.forEach((file) => {
    formData.append('files', file);
  });

  const response = await api.post<ApiResponse<UploadResult>>(
    '/files',
    formData,
  );

  return response.data;
};