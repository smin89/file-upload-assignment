import { useEffect, useState } from 'react';

import { getExtensions } from '../api/extensionApi';
import { getSetting } from '../api/settingApi';
import { uploadFiles } from '../api/fileApi';

import FileDropZone from '../components/file/FileDropZone';
import FileItem from '../components/file/FileItem';

import type { ExtensionSetting } from '../types/extension';
import type { UploadSetting } from '../types/setting';
import type { UploadedFile } from '../types/file';

import { formatFileSize, validateFile, isSameFile} from '../utils/fileUtils';
import { getApiErrorMessage } from '../utils/apiUtils';

function FileUploadPage() {
  const [files, setFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [setting, setSetting] = useState<UploadSetting | null>(null);
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([]);

  const [extensionSetting, setExtensionSetting] =
    useState<ExtensionSetting | null>(null);

  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadUploadPolicy = async () => {
      try {
        setLoading(true);

        const [settingData, extensionData] = await Promise.all([
          getSetting(),
          getExtensions(),
        ]);

        setSetting(settingData);
        setExtensionSetting(extensionData);
      } catch (error) {
        console.error(error);
        setError(
          getApiErrorMessage(
            error,
            '업로드 설정 정보를 불러오지 못했습니다.',
          ),
        );
      } finally {
        setLoading(false);
      }
    };

    loadUploadPolicy();
  }, []);

  const handleFilesSelected = (selectedFiles: File[]) => {
    setError('');
    setMessage('');
    setUploadedFiles([]);

    if (!setting || !extensionSetting) {
      setError(
        getApiErrorMessage(
          error,
          '업로드 설정 정보를 불러오지 못했습니다.',
        ),
      );
      return;
    }

    const errorMessages: string[] = [];
    const uniqueFiles: File[] = [];

    // 1. 중복 파일 검사
    selectedFiles.forEach((file) => {
      const duplicatedInCurrentFiles = files.some((currentFile) =>
        isSameFile(currentFile, file),
      );

      const duplicatedInSelectedFiles = uniqueFiles.some((selectedFile) =>
        isSameFile(selectedFile, file),
      );

      if (duplicatedInCurrentFiles || duplicatedInSelectedFiles) {
        errorMessages.push(
          `${file.name}: 이미 선택된 파일입니다.`,
        );
        return;
      }

      uniqueFiles.push(file);
    });

    // 2. 파일별 정책 검증
    const validFiles: File[] = [];

    uniqueFiles.forEach((file) => {
      const result = validateFile(
        file,
        setting,
        extensionSetting,
      );

      if (!result.valid) {
        errorMessages.push(
          `${file.name}: ${result.message}`,
        );
        return;
      }

      validFiles.push(file);
    });

    // 3. 현재 추가할 수 있는 파일 개수 계산
    const remainingCount =
      setting.maxFileCount - files.length;

    if (remainingCount <= 0) {
      errorMessages.push(
        `파일은 최대 ${setting.maxFileCount}개까지 업로드할 수 있습니다.`,
      );

      setError(errorMessages.join('\n'));
      return;
    }

    // 4. 정상 파일 중 최대 개수까지만 추가
    const filesToAdd =
      validFiles.slice(0, remainingCount);

    // 5. 개수 초과 여부
    if (validFiles.length > remainingCount) {
      const exceededFiles =
        validFiles.slice(remainingCount);

      exceededFiles.forEach((file) => {
        errorMessages.push(
          `${file.name}: 최대 파일 개수(${setting.maxFileCount}개)를 초과하여 제외되었습니다.`,
        );
      });
    }

    // 6. 정상 파일 추가
    if (filesToAdd.length > 0) {
      setFiles((currentFiles) => [
        ...currentFiles,
        ...filesToAdd,
      ]);
    }

    // 7. 오류 메시지 표시
    if (errorMessages.length > 0) {
      setError(errorMessages.join('\n'));
    }
  };

  const handleRemoveFile = (index: number) => {
    setFiles((currentFiles) =>
      currentFiles.filter((_, fileIndex) => fileIndex !== index),
    );
  };

  const handleClearFiles = () => {
    setFiles([]);
    setMessage('');
    setError('');
  };

  const handleUpload = async () => {
    if (files.length === 0) {
      setError('업로드할 파일을 선택해주세요.');
      return;
    }

    try {
      setUploading(true);
      setError('');
      setMessage('');
      setUploadedFiles([]);

      const response = await uploadFiles(files);

      setUploadedFiles(response.data.files);

      setMessage(
        `${response.data.files.length}개 파일 업로드가 완료되었습니다.`,
      );

      setFiles([]);
    } catch (error) {
      console.error(error);
      setError(
        getApiErrorMessage(
          error,
          '파일 업로드에 실패했습니다.',
        ),
      );
    } finally {
      setUploading(false);
    }
  };

  return (
    <main>
      <h1>파일 업로드</h1>

      {setting && (
        <p>
          최대 {setting.maxFileCount}개 / 파일당 최대{' '}
          {formatFileSize(setting.maxFileSize)}
        </p>
      )}

      <FileDropZone
        onFilesSelected={handleFilesSelected}
        disabled={loading || uploading}
      />

      {error && (
        <p style={{ whiteSpace: 'pre-line' }}>
          {error}
        </p>
      )}

      {message && <p>{message}</p>}

      {uploadedFiles.length > 0 && (
        <section>
          <h2>업로드 완료</h2>

          {uploadedFiles.map((file) => (
            <div key={file.id}>
              <strong>{file.originalName}</strong>

              <div>
                {formatFileSize(file.sizeBytes)}
                {' / '}
                {file.mimeType}
              </div>
            </div>
          ))}
        </section>
      )}

      {files.length > 0 && (
        <section>
          <h2>선택한 파일 ({files.length})</h2>

          {files.map((file, index) => (
            <FileItem
              key={`${file.name}-${file.size}-${file.lastModified}-${index}`}
              file={file}
              disabled={uploading}
              onRemove={() => handleRemoveFile(index)}
            />
          ))}

          <div>
            <button
              type="button"
              disabled={uploading}
              onClick={handleClearFiles}
            >
              전체 삭제
            </button>

            <button
              type="button"
              disabled={uploading}
              onClick={handleUpload}
            >
              {uploading ? '업로드 중...' : '업로드'}
            </button>
          </div>
        </section>
      )}
    </main>
  );
}

export default FileUploadPage;