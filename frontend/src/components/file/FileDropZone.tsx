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
      role="button"
      tabIndex={0}
      onClick={handleClick}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          handleClick();
        }
      }}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      style={{
        padding: '40px',
        border: `2px dashed ${dragging ? '#333' : '#aaa'}`,
        borderRadius: '8px',
        textAlign: 'center',
        cursor: disabled ? 'not-allowed' : 'pointer',
      }}
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

      <p>파일을 이곳에 드래그하거나 클릭하여 선택하세요.</p>

      <button
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