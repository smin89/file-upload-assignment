export interface ApiResponse<T> {
  resultCode: string;
  message: string;
  data: T;
}

export interface ApiErrorResponse {
  resultCode: string;
  message: string;
  data?: unknown;
}