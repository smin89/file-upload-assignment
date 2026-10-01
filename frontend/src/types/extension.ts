export interface FixedExtension {
  id: number;
  extension: string;
  enabled: boolean;
}

export interface CustomExtension {
  id: number;
  extension: string;
}

export interface ExtensionSetting {
  totalExtCount: number;
  fixedExtensions: FixedExtension[];
  customExtensions: CustomExtension[];
  customExtensionCount: number;
  customExtensionLimit: number;
}

export interface UpdateFixedExtensionRequest {
  id: number;
  enabled: boolean;
}

export interface CreateCustomExtensionRequest {
  extension: string;
}
