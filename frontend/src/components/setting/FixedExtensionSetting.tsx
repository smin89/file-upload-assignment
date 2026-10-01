import type { FixedExtension } from '../../types/extension';

interface FixedExtensionSettingProps {
  extensions: FixedExtension[];
  onChange: (id: number, enabled: boolean) => void;
}

function FixedExtensionSetting({
  extensions,
  onChange,
}: FixedExtensionSettingProps) {
  return (
    <section>
      <h2>고정 제한 확장자</h2>

      <div>
        {extensions.map((item) => (
          <label key={item.id}>
            <input
              type="checkbox"
              checked={item.enabled}
              onChange={(event) =>
                onChange(item.id, event.target.checked)
              }
            />

            {item.extension}
          </label>
        ))}
      </div>
    </section>
  );
}

export default FixedExtensionSetting;