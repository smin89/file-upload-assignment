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
  return (
    <div>
      <span>{file.name}</span>
      {' - '}
      <span>{formatFileSize(file.size)}</span>

      <button
        type="button"
        disabled={disabled}
        onClick={onRemove}
      >
        삭제
      </button>
    </div>
  );
}

export default FileItem;