import { useEffect, useRef, useState } from 'react';

import {
  createCustomExtension,
  deleteCustomExtension,
  getExtensions,
  updateFixedExtension,
} from '@/api/extensionApi';
import { getSetting, updateSetting } from '@/api/settingApi';
import CustomExtensionSetting from '@/components/setting/CustomExtensionSetting';
import FixedExtensionSetting from '@/components/setting/FixedExtensionSetting';
import UploadLimitSetting from '@/components/setting/UploadLimitSetting';
import { getApiErrorMessage } from '@/utils/apiUtils';
import {
  EXTENSION_LENGTH_ERROR,
  MAX_EXTENSION_LENGTH,
  normalizeExtension,
} from '@/utils/extensionUtils';

import type { ExtensionSetting } from '@/types/extension';
import type { UploadSetting } from '@/types/setting';

import '@/styles/pages/SettingsPage.css';

function SettingsPage() {
  const [extensionSetting, setExtensionSetting] = useState<ExtensionSetting | null>(null);

  const [setting, setSetting] = useState<UploadSetting | null>(null);

  const [customExtension, setCustomExtension] = useState('');

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [updatingExtensions, setUpdatingExtensions] = useState(false);
  const extensionRequestPending = useRef(false);
  const settingRequestPending = useRef(false);
  const [error, setError] = useState('');

  useEffect(() => {
    // 화면 이동 또는 StrictMode 재실행으로 끝난 요청은 상태에 반영하지 않는다.
    let active = true;
    const loadSettings = async () => {
      try {
        setLoading(true);
        setError('');

        const [extensionData, settingData] = await Promise.all([getExtensions(), getSetting()]);

        if (!active) return;

        setExtensionSetting(extensionData);
        setSetting(settingData);
      } catch (error) {
        if (!active) return;
        console.error(error);
        setError(getApiErrorMessage(error, '설정 정보를 불러오지 못했습니다.'));
      } finally {
        if (active) setLoading(false);
      }
    };

    loadSettings();

    return () => {
      active = false;
    };
  }, []);

  const handleFixedExtensionChange = async (id: number, enabled: boolean) => {
    // 변경 후 재조회까지 직렬화해 중복 요청과 이전 응답의 덮어쓰기를 막는다.
    if (extensionRequestPending.current) return;
    extensionRequestPending.current = true;
    setUpdatingExtensions(true);

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
    } finally {
      extensionRequestPending.current = false;
      setUpdatingExtensions(false);
    }
  };

  const loadExtensions = async () => {
    const data = await getExtensions();
    setExtensionSetting(data);
  };

  const handleAddCustomExtension = async () => {
    const extension = normalizeExtension(customExtension);

    if (!extension) {
      setError('추가할 확장자를 입력해주세요.');
      return;
    }

    if (extension.length > MAX_EXTENSION_LENGTH) {
      setError(EXTENSION_LENGTH_ERROR);
      return;
    }

    // 변경 후 재조회까지 직렬화해 중복 요청과 이전 응답의 덮어쓰기를 막는다.
    if (extensionRequestPending.current) return;
    extensionRequestPending.current = true;
    setUpdatingExtensions(true);

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
    } finally {
      extensionRequestPending.current = false;
      setUpdatingExtensions(false);
    }
  };

  const handleDeleteCustomExtension = async (id: number) => {
    // 변경 후 재조회까지 직렬화해 중복 요청과 이전 응답의 덮어쓰기를 막는다.
    if (extensionRequestPending.current) return;
    extensionRequestPending.current = true;
    setUpdatingExtensions(true);

    try {
      setError('');

      await deleteCustomExtension(id);

      await loadExtensions();
    } catch (error) {
      console.error(error);
      setError(getApiErrorMessage(error, '확장자 삭제에 실패했습니다.'));
    } finally {
      extensionRequestPending.current = false;
      setUpdatingExtensions(false);
    }
  };

  const handleSaveSetting = async () => {
    if (!setting || settingRequestPending.current) {
      return;
    }

    if (
      !Number.isSafeInteger(setting.maxFileSize) ||
      setting.maxFileSize < 1 ||
      !Number.isInteger(setting.maxFileCount) ||
      setting.maxFileCount < 1 ||
      setting.maxFileCount > 2147483647
    ) {
      setError('파일 크기는 1 byte 이상, 파일 개수는 1~2,147,483,647 사이의 정수여야 합니다.');
      return;
    }

    settingRequestPending.current = true;
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
      settingRequestPending.current = false;
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
            {error || '설정 정보를 불러올 수 없습니다.'}
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
            disabled={updatingExtensions}
            extensions={extensionSetting.fixedExtensions}
            onChange={handleFixedExtensionChange}
          />

          <CustomExtensionSetting
            disabled={updatingExtensions}
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
