import { useRef, useState } from 'react';

interface FileDropZoneProps {
  onFilesSelected: (files: File[]) => void;
  disabled?: boolean;
}

function FileDropZone({
  onFilesSelected,
  disabled = false,
}: FileDropZoneProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);

  const handleFiles = (fileList: FileList | null) => {
    if (!fileList || disabled) {
      return;
    }

    onFilesSelected(Array.from(fileList));
  };

  const handleDragOver = (
    event: React.DragEvent<HTMLDivElement>,
  ) => {
    event.preventDefault();

    if (!disabled) {
      setDragging(true);
    }
  };

  const handleDragLeave = () => {
    setDragging(false);
  };

  const handleDrop = (
    event: React.DragEvent<HTMLDivElement>,
  ) => {
    event.preventDefault();
    setDragging(false);

    handleFiles(event.dataTransfer.files);
  };

  const handleClick = () => {
    if (!disabled) {
      inputRef.current?.click();
    }
  };

  return (
    <div
      className={[
        'file-drop-zone',
        dragging ? 'file-drop-zone--dragging' : '',
        disabled ? 'file-drop-zone--disabled' : '',
      ]
        .filter(Boolean)
        .join(' ')}
      role="button"
      tabIndex={disabled ? -1 : 0}
      onClick={handleClick}
      onKeyDown={(event) => {
        if (
          !disabled &&
          (event.key === 'Enter' || event.key === ' ')
        ) {
          event.preventDefault();
          handleClick();
        }
      }}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      <input
        ref={inputRef}
        type="file"
        multiple
        hidden
        disabled={disabled}
        onChange={(event) => {
          handleFiles(event.target.files);

          // 동일한 파일을 다시 선택할 수 있도록 초기화
          event.target.value = '';
        }}
      />

      <div className="file-drop-zone__icon">
        ↑
      </div>

      <div className="file-drop-zone__content">
        <strong className="file-drop-zone__title">
          파일을 이곳에 드래그하세요
        </strong>

        <p className="file-drop-zone__description">
          또는 아래 버튼을 눌러 파일을 선택할 수 있습니다.
        </p>
      </div>

      <button
        className="file-drop-zone__button"
        type="button"
        disabled={disabled}
        onClick={(event) => {
          event.stopPropagation();
          handleClick();
        }}
      >
        파일 선택
      </button>
    </div>
  );
}

export default FileDropZone;