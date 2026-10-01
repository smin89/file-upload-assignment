import { formatFileSize } from '../../utils/fileUtils';

interface FileItemProps {
  file: File;
  onRemove: () => void;
  disabled?: boolean;
}

function FileItem({
  file,
  onRemove,
  disabled = false,
}: FileItemProps) {
  const getExtension = (fileName: string) => {
    const extension = fileName.split('.').pop();

    if (!extension || extension === fileName) {
      return 'FILE';
    }

    return extension.toUpperCase();
  };

  return (
    <div className="file-item">
      <div className="file-item__icon">
        <span>📄</span>
      </div>

      <div className="file-item__info">
        <div className="file-item__name" title={file.name}>
          {file.name}
        </div>

        <div className="file-item__meta">
          {getExtension(file.name)} · {formatFileSize(file.size)}
        </div>
      </div>

      <button
        type="button"
        className="file-item__remove"
        onClick={onRemove}
        disabled={disabled}
        aria-label={`${file.name} 삭제`}
      >
        삭제
      </button>
    </div>
  );
}

export default FileItem;