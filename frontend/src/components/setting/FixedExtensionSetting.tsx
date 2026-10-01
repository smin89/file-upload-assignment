import type { FixedExtension } from '@/types/extension';

interface FixedExtensionSettingProps {
  disabled: boolean;
  extensions: FixedExtension[];
  onChange: (id: number, enabled: boolean) => void;
}

function FixedExtensionSetting({ disabled, extensions, onChange }: FixedExtensionSettingProps) {
  return (
    <section className="settings-card">
      <div className="settings-card__header">
        <div>
          <h2 className="settings-card__title">고정 제한 확장자</h2>

          <p className="settings-card__description">선택한 확장자의 파일 업로드를 제한합니다.</p>
        </div>
      </div>

      <div className="fixed-extensions">
        {extensions.map((item) => (
          <label
            key={item.id}
            className={['fixed-extension', item.enabled ? 'fixed-extension--checked' : '']
              .filter(Boolean)
              .join(' ')}
          >
            <input
              type="checkbox"
              disabled={disabled}
              className="fixed-extension__checkbox"
              checked={item.enabled}
              onChange={(event) => onChange(item.id, event.target.checked)}
            />

            <span className="fixed-extension__name">.{item.extension}</span>
          </label>
        ))}
      </div>
    </section>
  );
}

export default FixedExtensionSetting;
