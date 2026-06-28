import { motion, AnimatePresence } from 'framer-motion';
import { Sparkles, Trash2, Clock, CheckCircle, XCircle, ImageIcon } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Sidebar({ images, selectedId, onSelect, onDelete, loading }) {
  const { user, logout } = useAuth();

  return (
    <aside className="w-72 h-screen flex flex-col bg-dark-800/60 backdrop-blur-xl border-r border-white/5">
      {/* Logo */}
      <div className="p-5 border-b border-white/5">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-violet-600 to-purple-600 flex items-center justify-center shadow-glow-sm">
            <Sparkles className="w-5 h-5 text-white" />
          </div>
          <div>
            <h1 className="font-bold text-white text-sm">VisionForge AI</h1>
            <p className="text-[10px] text-slate-500">Image Generation</p>
          </div>
        </div>
      </div>

      {/* User info + credits */}
      <div className="p-4 border-b border-white/5">
        <div className="flex items-center gap-3 p-3 rounded-xl bg-dark-700/50">
          <div className="w-9 h-9 rounded-full bg-gradient-to-br from-violet-500 to-pink-500 flex items-center justify-center text-white font-bold text-sm">
            {user?.fullName?.charAt(0)?.toUpperCase() || 'U'}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-white text-sm font-medium truncate">{user?.fullName}</p>
            <p className="text-slate-500 text-xs truncate">{user?.email}</p>
          </div>
        </div>
        <div className="mt-3 flex items-center justify-between px-1">
          <span className="text-xs text-slate-500">Credits remaining</span>
          <span className="text-xs font-bold text-violet-400">{user?.credits ?? '—'}</span>
        </div>
        <div className="mt-1.5 h-1.5 rounded-full bg-dark-600 overflow-hidden">
          <div
            className="h-full rounded-full bg-gradient-to-r from-violet-600 to-purple-500 transition-all duration-500"
            style={{ width: `${Math.min(100, ((user?.credits ?? 0) / 30) * 100)}%` }}
          />
        </div>
      </div>

      {/* Image history */}
      <div className="flex-1 overflow-y-auto p-3">
        <p className="text-[11px] font-semibold text-slate-500 uppercase tracking-widest mb-3 px-1">
          Your Gallery
        </p>

        {loading ? (
          <div className="space-y-2">
            {[1, 2, 3].map(i => (
              <div key={i} className="h-20 rounded-xl shimmer-bg" />
            ))}
          </div>
        ) : images.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <ImageIcon className="w-10 h-10 text-slate-700 mb-3" />
            <p className="text-slate-600 text-sm">No images yet</p>
            <p className="text-slate-700 text-xs mt-1">Generate your first masterpiece!</p>
          </div>
        ) : (
          <div className="space-y-2">
            <AnimatePresence>
              {images.map((img) => (
                <motion.div
                  key={img.id}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  onClick={() => onSelect(img)}
                  className={`group relative flex gap-3 p-2.5 rounded-xl cursor-pointer transition-all duration-200 ${
                    selectedId === img.id
                      ? 'bg-violet-600/20 border border-violet-500/40'
                      : 'hover:bg-dark-600/80 border border-transparent'
                  }`}
                >
                  {/* Thumbnail */}
                  <div className="w-14 h-14 rounded-lg overflow-hidden shrink-0 bg-dark-600">
                    {img.imageDataUrl ? (
                      <img src={img.imageDataUrl} alt="" className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center">
                        {img.status === 'FAILED'
                          ? <XCircle className="w-5 h-5 text-red-500" />
                          : <Clock className="w-5 h-5 text-slate-600" />
                        }
                      </div>
                    )}
                  </div>

                  {/* Info */}
                  <div className="flex-1 min-w-0">
                    <p className="text-xs text-slate-300 line-clamp-2 leading-tight">
                      {img.prompt}
                    </p>
                    <div className="flex items-center gap-1 mt-1.5">
                      {img.status === 'SUCCESS'
                        ? <CheckCircle className="w-3 h-3 text-emerald-500" />
                        : img.status === 'FAILED'
                          ? <XCircle className="w-3 h-3 text-red-500" />
                          : <Clock className="w-3 h-3 text-yellow-500" />
                      }
                      <span className={`text-[10px] font-medium ${
                        img.status === 'SUCCESS' ? 'text-emerald-500'
                        : img.status === 'FAILED' ? 'text-red-500'
                        : 'text-yellow-500'
                      }`}>{img.status}</span>
                    </div>
                  </div>

                  {/* Delete button */}
                  <button
                    onClick={(e) => { e.stopPropagation(); onDelete(img.id); }}
                    className="absolute right-2 top-2 opacity-0 group-hover:opacity-100 p-1 rounded-lg bg-dark-500 hover:bg-red-500/20 hover:text-red-400 text-slate-500 transition-all duration-200"
                  >
                    <Trash2 size={12} />
                  </button>
                </motion.div>
              ))}
            </AnimatePresence>
          </div>
        )}
      </div>

      {/* Logout */}
      <div className="p-4 border-t border-white/5">
        <button
          onClick={logout}
          className="w-full py-2.5 rounded-xl text-sm text-slate-500 hover:text-slate-300 hover:bg-dark-600 transition-all duration-200"
        >
          Sign out
        </button>
      </div>
    </aside>
  );
}
