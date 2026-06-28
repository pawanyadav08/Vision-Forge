import { motion, AnimatePresence } from 'framer-motion';
import { Download, Trash2, ImageIcon, Sparkles, Loader2, XCircle, Info } from 'lucide-react';

export default function ImageDisplay({ image, generating, generatingPrompt, onDelete }) {

  const handleDownload = () => {
    if (!image?.imageDataUrl) return;
    const a = document.createElement('a');
    a.href = image.imageDataUrl;
    a.download = `visionforge-${image.id}.png`;
    a.click();
  };

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="p-6 border-b border-white/5">
        <h2 className="text-xl font-bold text-white flex items-center gap-2">
          <ImageIcon className="w-5 h-5 text-violet-400" />
          Result
        </h2>
      </div>

      <div className="flex-1 overflow-y-auto p-6 flex flex-col">
        <AnimatePresence mode="wait">

          {/* Generating state */}
          {generating && (
            <motion.div
              key="generating"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="flex-1 flex flex-col items-center justify-center text-center"
            >
              <div className="relative mb-8">
                {/* Outer glow rings */}
                <div className="absolute inset-0 rounded-full bg-violet-600/20 animate-ping" />
                <div className="absolute -inset-4 rounded-full border border-violet-500/20 animate-pulse" />
                <div className="w-24 h-24 rounded-full bg-gradient-to-br from-violet-600 to-purple-600 flex items-center justify-center shadow-glow-xl">
                  <Sparkles className="w-12 h-12 text-white animate-pulse" />
                </div>
              </div>

              <h3 className="text-xl font-bold text-white mb-2">Creating your masterpiece...</h3>
              <p className="text-slate-500 text-sm max-w-xs leading-relaxed mb-6">
                The AI is processing your prompt. This usually takes 30–120 seconds.
              </p>

              {generatingPrompt && (
                <div className="w-full max-w-sm p-4 rounded-xl bg-dark-700/50 border border-dark-400 text-left">
                  <p className="text-xs text-slate-500 mb-1">Prompt:</p>
                  <p className="text-sm text-slate-300 italic">"{generatingPrompt}"</p>
                </div>
              )}

              <div className="flex items-center gap-2 mt-6 text-slate-500 text-sm">
                <Loader2 size={16} className="animate-spin text-violet-400" />
                <span>Powered by HuggingFace SDXL</span>
              </div>
            </motion.div>
          )}

          {/* No image selected / empty */}
          {!generating && !image && (
            <motion.div
              key="empty"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="flex-1 flex flex-col items-center justify-center text-center"
            >
              <div className="w-32 h-32 rounded-3xl bg-dark-700/50 border-2 border-dashed border-dark-400 flex items-center justify-center mb-6 animate-float">
                <Sparkles className="w-12 h-12 text-dark-400" />
              </div>
              <h3 className="text-lg font-semibold text-slate-500 mb-2">No image yet</h3>
              <p className="text-slate-600 text-sm max-w-xs">
                Write a prompt on the left and click Generate to create your first AI image!
              </p>
            </motion.div>
          )}

          {/* Image displayed */}
          {!generating && image && (
            <motion.div
              key={image.id}
              initial={{ opacity: 0, scale: 0.96 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.96 }}
              transition={{ duration: 0.4 }}
              className="flex-1 flex flex-col"
            >
              {/* Image */}
              <div className="relative rounded-2xl overflow-hidden bg-dark-700 shadow-glow-sm mb-4 group">
                {image.status === 'SUCCESS' && image.imageDataUrl ? (
                  <img
                    src={image.imageDataUrl}
                    alt={image.prompt}
                    className="w-full object-contain max-h-[60vh]"
                  />
                ) : image.status === 'FAILED' ? (
                  <div className="flex flex-col items-center justify-center py-20 text-center">
                    <XCircle className="w-12 h-12 text-red-500 mb-3" />
                    <p className="text-red-400 font-medium">Generation Failed</p>
                    <p className="text-slate-500 text-sm mt-1 max-w-xs">{image.failureReason || 'Unknown error'}</p>
                  </div>
                ) : (
                  <div className="flex items-center justify-center py-20">
                    <Loader2 className="w-10 h-10 text-violet-400 animate-spin" />
                  </div>
                )}
              </div>

              {/* Action buttons */}
              {image.status === 'SUCCESS' && (
                <div className="flex gap-3 mb-4">
                  <button onClick={handleDownload}
                    className="btn-primary flex-1 py-2.5 flex items-center justify-center gap-2 text-sm">
                    <Download size={16} />
                    Download PNG
                  </button>
                  <button onClick={() => onDelete(image.id)}
                    className="btn-secondary flex items-center justify-center gap-2 px-4 text-sm text-red-400 hover:text-red-300 hover:border-red-500/50">
                    <Trash2 size={16} />
                  </button>
                </div>
              )}

              {/* Metadata */}
              <div className="glass-card p-4 space-y-3">
                <div className="flex items-start gap-2">
                  <Info size={14} className="text-violet-400 mt-0.5 shrink-0" />
                  <p className="text-sm text-slate-300 leading-relaxed">{image.prompt}</p>
                </div>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="p-2 rounded-lg bg-dark-700/50">
                    <p className="text-slate-500">Model</p>
                    <p className="text-slate-300 font-medium truncate">{image.modelUsed?.split('/').pop()}</p>
                  </div>
                  <div className="p-2 rounded-lg bg-dark-700/50">
                    <p className="text-slate-500">Size</p>
                    <p className="text-slate-300 font-medium">{image.width} × {image.height}</p>
                  </div>
                  <div className="p-2 rounded-lg bg-dark-700/50">
                    <p className="text-slate-500">Provider</p>
                    <p className="text-slate-300 font-medium">{image.provider}</p>
                  </div>
                  <div className="p-2 rounded-lg bg-dark-700/50">
                    <p className="text-slate-500">Created</p>
                    <p className="text-slate-300 font-medium">
                      {image.createdAt ? new Date(image.createdAt).toLocaleTimeString() : '—'}
                    </p>
                  </div>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
