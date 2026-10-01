import { useEffect, useState } from 'react';
import {
  createCustomExtension,
  deleteCustomExtension,
  getExtensions,
  updateFixedExtension,
} from '../api/extensionApi';
import {
  getSetting,
  updateSetting,
} from '../api/settingApi';
import type { ExtensionSetting } from '../types/extension';
import type { UploadSetting } from '../types/setting';
import FixedExtensionSetting from '../components/setting/FixedExtensionSetting';
import CustomExtensionSetting from '../components/setting/CustomExtensionSetting';
import UploadLimitSetting from '../components/setting/UploadLimitSetting';
import { getApiErrorMessage } from '../utils/apiUtils';

function SettingsPage() {
  const [extensionSetting, setExtensionSetting] =
    useState<ExtensionSetting | null>(null);

  const [setting, setSetting] =
    useState<UploadSetting | null>(null);

  const [customExtension, setCustomExtension] = useState('');

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const loadSettings = async () => {
      try {
        setLoading(true);
        setError('');

        const [extensionData, settingData] = await Promise.all([
          getExtensions(),
          getSetting(),
        ]);

        setExtensionSetting(extensionData);
        setSetting(settingData);
      } catch (error) {
        console.error(error);
        setError(
          getApiErrorMessage(
            error,
            '설정 정보를 불러오지 못했습니다.',
          ),
        );
      } finally {
        setLoading(false);
      }
    };

    loadSettings();
  }, []);

  const handleFixedExtensionChange = async (
    id: number,
    enabled: boolean,
  ) => {
    try {
      setError('');

      await updateFixedExtension({
        id,
        enabled,
      });

      await loadExtensions();
    } catch (error) {
      console.error(error);
      setError(
        getApiErrorMessage(
          error,
          '고정 확장자 설정 변경에 실패했습니다.',
        ),
      );
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
      setError(
        getApiErrorMessage(
          error,
          '확장자 추가에 실패했습니다.',
        ),
      );
    }
  };

  const handleDeleteCustomExtension = async (id: number) => {
    try {
      setError('');

      await deleteCustomExtension(id);

      await loadExtensions();
    } catch (error) {
      console.error(error);
      setError(
        getApiErrorMessage(
          error,
          '확장자 삭제에 실패했습니다.',
        ),
      );
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
      setError(
        getApiErrorMessage(
          error,
          '설정 저장에 실패했습니다.',
        ),
      );
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div>설정 정보를 불러오는 중입니다...</div>;
  }

  if (!extensionSetting || !setting) {
    return <div>설정 정보를 불러올 수 없습니다.</div>;
  }

  return (
    <main>
      <h1>업로드 설정</h1>

      {error && <p>{error}</p>}

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
    </main>
  );
}

export default SettingsPage;