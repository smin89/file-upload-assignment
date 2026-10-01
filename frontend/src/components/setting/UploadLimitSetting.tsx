import type { UploadSetting } from '../../types/setting';

interface UploadLimitSettingProps {
  setting: UploadSetting;
  saving: boolean;
  onChange: (setting: UploadSetting) => void;
  onSave: () => void;
}

function UploadLimitSetting({ setting, saving, onChange, onSave }: UploadLimitSettingProps) {
  return (
    <section className="settings-card">
      <div className="settings-card__header">
        <div>
          <h2 className="settings-card__title">업로드 제한</h2>

          <p className="settings-card__description">
            한 번에 업로드할 수 있는 파일의 크기와 개수를 설정합니다.
          </p>
        </div>
      </div>

      <div className="upload-limit">
        <div className="upload-limit__field">
          <label className="upload-limit__label" htmlFor="max-file-size">
            최대 파일 크기
          </label>

          <p className="upload-limit__help">파일 하나당 허용할 최대 크기입니다.</p>

          <div className="upload-limit__input-wrapper">
            <input
              id="max-file-size"
              type="number"
              className="upload-limit__input"
              min={1}
              value={setting.maxFileSize}
              onChange={(event) =>
                onChange({
                  ...setting,
                  maxFileSize: Number(event.target.value),
                })
              }
            />

            <span className="upload-limit__unit">MB</span>
          </div>
        </div>

        <div className="upload-limit__field">
          <label className="upload-limit__label" htmlFor="max-file-count">
            최대 업로드 개수
          </label>

          <p className="upload-limit__help">
            한 번에 선택하여 업로드할 수 있는 최대 파일 개수입니다.
          </p>

          <div className="upload-limit__input-wrapper">
            <input
              id="max-file-count"
              type="number"
              className="upload-limit__input"
              min={1}
              value={setting.maxFileCount}
              onChange={(event) =>
                onChange({
                  ...setting,
                  maxFileCount: Number(event.target.value),
                })
              }
            />

            <span className="upload-limit__unit">개</span>
          </div>
        </div>
      </div>

      <div className="upload-limit__actions">
        <button type="button" className="upload-limit__save" disabled={saving} onClick={onSave}>
          {saving ? '저장 중...' : '설정 저장'}
        </button>
      </div>
    </section>
  );
}

export default UploadLimitSetting;
