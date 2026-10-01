import { NavLink, Outlet } from 'react-router-dom';

import '@/styles/common.css';

function Layout() {
  return (
    <div className="app">
      <header className="app-header">
        <div className="header-inner">
          <NavLink to="/upload" className="logo">
            File Upload
          </NavLink>

          <nav className="navigation">
            <NavLink
              to="/upload"
              className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
            >
              파일 업로드
            </NavLink>

            <NavLink
              to="/settings"
              className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
            >
              설정
            </NavLink>
          </nav>
        </div>
      </header>

      <div className="content">
        <Outlet />
      </div>
    </div>
  );
}

export default Layout;
