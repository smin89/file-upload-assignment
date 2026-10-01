import { useState } from 'react';
import FileDropZone from '../components/file/FileDropZone';
import FileItem from '../components/file/FileItem';
import { uploadFiles } from '../api/fileApi';

function FileUploadPage() {
  const [files, setFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const handleFilesSelected = (selectedFiles: File[]) => {
    setError('');
    setMessage('');

    setFiles((currentFiles) => [
      ...currentFiles,
      ...selectedFiles,
    ]);
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

      const response = await uploadFiles(files);

      setMessage(response.message || '파일 업로드가 완료되었습니다.');

      setFiles([]);
    } catch (error) {
      console.error(error);
      setError('파일 업로드에 실패했습니다.');
    } finally {
      setUploading(false);
    }
  };

  return (
    <main>
      <h1>파일 업로드</h1>

      <FileDropZone
        onFilesSelected={handleFilesSelected}
        disabled={uploading}
      />

      {error && <p>{error}</p>}

      {message && <p>{message}</p>}

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