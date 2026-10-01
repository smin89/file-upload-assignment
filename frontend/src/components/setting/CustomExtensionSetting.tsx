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
  const isLimitReached = count >= limit;

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!value.trim() || isLimitReached) {
      return;
    }

    onAdd();
  };

  return (
    <section className="settings-card">
      <div className="settings-card__header">
        <div>
          <h2 className="settings-card__title">추가 제한 확장자</h2>

          <p className="settings-card__description">
            추가로 업로드를 제한할 확장자를 등록할 수 있습니다.
          </p>
        </div>

        <span className="custom-extension__count">
          {count} / {limit}
        </span>
      </div>

      <form className="custom-extension__form" onSubmit={handleSubmit}>
        <div className="custom-extension__input-wrapper">
          <span className="custom-extension__prefix">.</span>

          <input
            type="text"
            className="custom-extension__input"
            value={value}
            placeholder="확장자 입력"
            disabled={isLimitReached}
            onChange={(event) => onValueChange(event.target.value)}
          />
        </div>

        <button
          type="submit"
          className="custom-extension__add"
          disabled={!value.trim() || isLimitReached}
        >
          추가
        </button>
      </form>

      {isLimitReached && (
        <p className="custom-extension__limit-message">
          추가 제한 확장자는 최대 {limit}개까지 등록할 수 있습니다.
        </p>
      )}

      {extensions.length > 0 ? (
        <div className="custom-extension__list">
          {extensions.map((item) => (
            <div key={item.id} className="custom-extension__tag">
              <span>.{item.extension}</span>

              <button
                type="button"
                className="custom-extension__delete"
                aria-label={`${item.extension} 확장자 삭제`}
                onClick={() => onDelete(item.id)}
              >
                ×
              </button>
            </div>
          ))}
        </div>
      ) : (
        <div className="custom-extension__empty">등록된 추가 제한 확장자가 없습니다.</div>
      )}
    </section>
  );
}

export default CustomExtensionSetting;
