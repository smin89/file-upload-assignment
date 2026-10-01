export interface ApiResponse<T> {
  resultCode: string;
  message: string;
  data: T;
}