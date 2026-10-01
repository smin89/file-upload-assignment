import type { UploadSetting } from '../../types/setting';

interface UploadLimitSettingProps {
  setting: UploadSetting;
  saving: boolean;
  onChange: (setting: UploadSetting) => void;
  onSave: () => void;
}

function UploadLimitSetting({
  setting,
  saving,
  onChange,
  onSave,
}: UploadLimitSettingProps) {
  return (
    <section>
      <h2>파일 업로드 제한</h2>

      <div>
        <label htmlFor="maxFileCount">
          최대 파일 개수
        </label>

        <input
          id="maxFileCount"
          type="number"
          min="1"
          value={setting.maxFileCount}
          onChange={(event) =>
            onChange({
              ...setting,
              maxFileCount: Number(event.target.value),
            })
          }
        />
      </div>

      <div>
        <label htmlFor="maxFileSize">
          최대 파일 크기 (MB)
        </label>

        <input
          id="maxFileSize"
          type="number"
          min="1"
          value={setting.maxFileSize / 1024 / 1024}
          onChange={(event) =>
            onChange({
              ...setting,
              maxFileSize:
                Number(event.target.value) * 1024 * 1024,
            })
          }
        />
      </div>

      <button
        type="button"
        disabled={saving}
        onClick={onSave}
      >
        {saving ? '저장 중...' : '설정 저장'}
      </button>
    </section>
  );
}

export default UploadLimitSetting;