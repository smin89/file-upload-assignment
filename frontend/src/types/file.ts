export interface FileValidationResult {
  valid: boolean;
  message?: string;
}

export interface UploadedFile {
  id: number;
  originalName: string;
  mimeType: string;
  sizeBytes: number;
  createdAt: string;
}

export interface UploadResult {
  files: UploadedFile[];
}
