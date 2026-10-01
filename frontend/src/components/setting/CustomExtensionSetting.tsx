import type { CustomExtension } from '../../types/extension';

interface CustomExtensionSettingProps {
  extensions: CustomExtension[];
  count: number;
  limit: number;
  value: string;
  onValueChange: (value: string) => void;
  onAdd: () => void;
  onDelete: (id: number) => void;
}

function CustomExtensionSetting({
  extensions,
  count,
  limit,
  value,
  onValueChange,
  onAdd,
  onDelete,
}: CustomExtensionSettingProps) {
  return (
    <section>
      <h2>커스텀 제한 확장자</h2>

      <div>
        <input
          type="text"
          value={value}
          placeholder="확장자 입력"
          onChange={(event) => onValueChange(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              onAdd();
            }
          }}
        />

        <button
          type="button"
          onClick={onAdd}
          disabled={count >= limit}
        >
          추가
        </button>
      </div>

      <p>
        {count} / {limit}
      </p>

      <div>
        {extensions.map((item) => (
          <span key={item.id}>
            {item.extension}

            <button
              type="button"
              onClick={() => onDelete(item.id)}
              aria-label={`${item.extension} 삭제`}
            >
              ×
            </button>
          </span>
        ))}
      </div>
    </section>
  );
}

export default CustomExtensionSetting;