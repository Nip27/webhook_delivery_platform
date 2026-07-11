import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: '📊' },
  { to: '/endpoints', label: 'Endpoints', icon: '🔗' },
  { to: '/subscriptions', label: 'Subscriptions', icon: '📋' },
  { to: '/deliveries', label: 'Delivery Logs', icon: '📦' },
  { to: '/api-keys', label: 'API Keys', icon: '🔑' },
];

export default function Sidebar() {
  const { user, logout } = useAuth();
  return (
    <aside className="w-64 bg-white border-r border-gray-200 flex flex-col">
      <div className="px-6 py-5 border-b border-gray-200">
        <h1 className="text-xl font-bold text-indigo-600">WebhookFlow</h1>
        <p className="text-xs text-gray-500 mt-1">Event Delivery Platform</p>
      </div>
      <nav className="flex-1 px-4 py-4 space-y-1">
        {navItems.map(({ to, label, icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors duration-150 ${
                isActive
                  ? 'bg-indigo-50 text-indigo-700'
                  : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
              }`
            }
          >
            <span>{icon}</span>
            {label}
          </NavLink>
        ))}
      </nav>
      <div className="px-4 py-4 border-t border-gray-200">
        <div className="text-xs text-gray-500 truncate mb-2">{user?.email}</div>
        <button
          onClick={logout}
          className="w-full text-left text-sm text-red-600 hover:text-red-800 px-2 py-1"
        >
          Sign out
        </button>
      </div>
    </aside>
  );
}
