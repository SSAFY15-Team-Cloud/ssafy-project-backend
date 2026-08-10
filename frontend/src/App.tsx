import { useEffect } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './lib/auth'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import DashboardPage from './pages/DashboardPage'
import JoinPage from './pages/JoinPage'
import PreJoinPage from './pages/PreJoinPage'
import MeetingPage from './pages/MeetingPage'
import ReportPage from './pages/ReportPage'
import KnowledgePage from './pages/KnowledgePage'

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { user, initialized } = useAuth()
  const location = useLocation()

  if (!initialized) {
    return (
      <div className="flex h-full items-center justify-center text-muted">
        불러오는 중…
      </div>
    )
  }
  if (!user) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }
  return <>{children}</>
}

export default function App() {
  const loadMe = useAuth((s) => s.loadMe)

  useEffect(() => {
    void loadMe()
  }, [loadMe])

  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/join/:roomCode" element={<JoinPage />} />
      <Route
        path="/dashboard"
        element={
          <RequireAuth>
            <DashboardPage />
          </RequireAuth>
        }
      />
      <Route
        path="/rooms/:roomId/prejoin"
        element={
          <RequireAuth>
            <PreJoinPage />
          </RequireAuth>
        }
      />
      <Route
        path="/rooms/:roomId/meet"
        element={
          <RequireAuth>
            <MeetingPage />
          </RequireAuth>
        }
      />
      <Route
        path="/rooms/:roomId/report"
        element={
          <RequireAuth>
            <ReportPage />
          </RequireAuth>
        }
      />
      <Route
        path="/knowledge"
        element={
          <RequireAuth>
            <KnowledgePage />
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
