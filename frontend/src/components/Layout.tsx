import { NavLink, Outlet } from 'react-router-dom'
import { FileUp, Users } from 'lucide-react'
import { cn } from '../lib/utils'

const navItems = [
  { to: '/upload', label: 'Upload', icon: FileUp },
  { to: '/candidates', label: 'Candidates', icon: Users },
]

export function Layout() {
  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-50 border-b border-border bg-white/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-8">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-brand-600">
                EightFold
              </p>
              <h1 className="text-lg font-bold text-foreground">Candidate Transformer</h1>
            </div>
            <nav className="hidden items-center gap-1 sm:flex">
              {navItems.map(({ to, label, icon: Icon }) => (
                <NavLink
                  key={to}
                  to={to}
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-brand-50 text-brand-700'
                        : 'text-muted-foreground hover:bg-slate-100 hover:text-foreground',
                    )
                  }
                >
                  <Icon className="h-4 w-4" />
                  {label}
                </NavLink>
              ))}
            </nav>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <Outlet />
      </main>
    </div>
  )
}
