import { useAuth } from './context/AuthContext';
import AuthPage from './pages/AuthPage';
import AppPage from './pages/AppPage';

export default function App() {
  const { isAuthenticated } = useAuth();

  return isAuthenticated ? <AppPage /> : <AuthPage />;
}
