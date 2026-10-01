import api from '@/api/axios';

import type { ApiResponse } from '@/types/api';
import type { UploadSetting, UpdateSettingRequest } from '@/types/setting';

export const getSetting = async (): Promise<UploadSetting> => {
  const response = await api.get<ApiResponse<UploadSetting>>('/setting');

  return response.data.data;
};

export const updateSetting = async (request: UpdateSettingRequest): Promise<UploadSetting> => {
  const response = await api.put<ApiResponse<UploadSetting>>('/setting', request);

  return response.data.data;
};
