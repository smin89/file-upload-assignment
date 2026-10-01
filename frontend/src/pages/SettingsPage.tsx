import { useEffect, useState } from 'react';
import {
  createCustomExtension,
  deleteCustomExtension,
  getExtensions,
  updateFixedExtension,
} from '../api/extensionApi';
import { getSetting, updateSetting } from '../api/settingApi';
import type { ExtensionSetting } from '../types/extension';
import type { UploadSetting } from '../types/setting';
import FixedExtensionSetting from '../components/setting/FixedExtensionSetting';
import CustomExtensionSetting from '../components/setting/CustomExtensionSetting';
import UploadLimitSetting from '../components/setting/UploadLimitSetting';
import { getApiErrorMessage } from '../utils/apiUtils';

function SettingsPage() {
  const [extensionSetting, setExtensionSetting] = useState<ExtensionSetting | null>(null);

  const [setting, setSetting] = useState<UploadSetting | null>(null);

  const [customExtension, setCustomExtension] = useState('');

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const loadSettings = async () => {
      try {
        setLoading(true);
        setError('');

        const [extensionData, settingData] = await Promise.all([getExtensions(), getSetting()]);

        setExtensionSetting(extensionData);
        setSetting(settingData);
      } catch (error) {
        console.error(error);
        setError(getApiErrorMessage(error, '설정 정보를 불러오지 못했습니다.'));
      } finally {
        setLoading(false);
      }
    };

    loadSettings();
  }, []);

  const handleFixedExtensionChange = async (id: number, enabled: boolean) => {
    try {
      setError('');

      await updateFixedExtension({
        id,
        enabled,
      });

      await loadExtensions();
    } catch (error) {
      console.error(error);
      setError(getApiErrorMessage(error, '고정 확장자 설정 변경에 실패했습니다.'));
    }
  };

  const loadExtensions = async () => {
    const data = await getExtensions();
    setExtensionSetting(data);
  };

  const handleAddCustomExtension = async () => {
    const extension = customExtension.trim();

    if (!extension) {
      setError('추가할 확장자를 입력해주세요.');
      return;
    }

    try {
      setError('');

      await createCustomExtension({
        extension,
      });

      setCustomExtension('');

      await loadExtensions();
    } catch (error) {
      console.error(error);
      setError(getApiErrorMessage(error, '확장자 추가에 실패했습니다.'));
    }
  };

  const handleDeleteCustomExtension = async (id: number) => {
    try {
      setError('');

      await deleteCustomExtension(id);

      await loadExtensions();
    } catch (error) {
      console.error(error);
      setError(getApiErrorMessage(error, '확장자 삭제에 실패했습니다.'));
    }
  };

  const handleSaveSetting = async () => {
    if (!setting) {
      return;
    }

    try {
      setSaving(true);
      setError('');

      const updatedSetting = await updateSetting(setting);

      setSetting(updatedSetting);

      alert('설정이 저장되었습니다.');
    } catch (error) {
      console.error(error);
      setError(getApiErrorMessage(error, '설정 저장에 실패했습니다.'));
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <main className="settings-page">
        <div className="settings-container">
          <div className="settings-state">설정 정보를 불러오는 중입니다...</div>
        </div>
      </main>
    );
  }

  if (!extensionSetting || !setting) {
    return (
      <main className="settings-page">
        <div className="settings-container">
          <div className="settings-state settings-state--error">
            설정 정보를 불러올 수 없습니다.
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="settings-page">
      <div className="settings-container">
        <header className="settings-header">
          <h1 className="settings-header__title">업로드 설정</h1>

          <p className="settings-header__description">
            파일 업로드 제한 및 확장자 정책을 설정할 수 있습니다.
          </p>
        </header>

        {error && (
          <div className="settings-error" role="alert">
            <div className="settings-error__icon">!</div>

            <div className="settings-error__content">
              <strong className="settings-error__title">요청을 처리할 수 없습니다.</strong>

              <p className="settings-error__message">{error}</p>
            </div>
          </div>
        )}

        <div className="settings-content">
          <FixedExtensionSetting
            extensions={extensionSetting.fixedExtensions}
            onChange={handleFixedExtensionChange}
          />

          <CustomExtensionSetting
            extensions={extensionSetting.customExtensions}
            count={extensionSetting.customExtensionCount}
            limit={extensionSetting.customExtensionLimit}
            value={customExtension}
            onValueChange={setCustomExtension}
            onAdd={handleAddCustomExtension}
            onDelete={handleDeleteCustomExtension}
          />

          <UploadLimitSetting
            setting={setting}
            saving={saving}
            onChange={setSetting}
            onSave={handleSaveSetting}
          />
        </div>
      </div>
    </main>
  );
}

export default SettingsPage;
