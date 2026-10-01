import { Navigate, Route, Routes } from 'react-router-dom';

import Layout from './components/common/Layout';
import FileUploadPage from './pages/FileUploadPage';
import SettingsPage from './pages/SettingsPage';

function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route
          path="/"
          element={<Navigate to="/upload" replace />}
        />

        <Route
          path="/upload"
          element={<FileUploadPage />}
        />

        <Route
          path="/settings"
          element={<SettingsPage />}
        />
      </Route>
    </Routes>
  );
}

export default App;