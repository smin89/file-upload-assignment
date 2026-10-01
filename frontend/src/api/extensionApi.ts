import api from '@/api/axios';

import type { ApiResponse } from '@/types/api';
import type {
  ExtensionSetting,
  UpdateFixedExtensionRequest,
  CreateCustomExtensionRequest,
} from '@/types/extension';

export const getExtensions = async (): Promise<ExtensionSetting> => {
  const response = await api.get<ApiResponse<ExtensionSetting>>('/extensions');

  return response.data.data;
};

export const updateFixedExtension = async (request: UpdateFixedExtensionRequest): Promise<void> => {
  await api.patch('/extensions/fixed', request);
};

export const createCustomExtension = async (
  request: CreateCustomExtensionRequest
): Promise<void> => {
  await api.post('/extensions/custom', request);
};

export const deleteCustomExtension = async (id: number): Promise<void> => {
  await api.delete(`/extensions/custom/${id}`);
};
