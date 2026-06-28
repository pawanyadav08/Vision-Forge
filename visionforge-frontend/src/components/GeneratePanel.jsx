import { useState } from 'react';
import { motion } from 'framer-motion';
import { Sparkles, Loader2, ChevronDown, ChevronUp, Wand2 } from 'lucide-react';

const EXAMPLE_PROMPTS = [
  "A majestic dragon soaring over snowy mountains at sunset, cinematic, 4K",
  "Cyberpunk city at night with neon lights reflecting on rain-wet streets",
  "An enchanted forest with glowing mushrooms and magical fireflies",
  "Astronaut floating in space with Earth in the background, photorealistic",
  "A serene Japanese garden with cherry blossoms and koi pond",
];

export default function GeneratePanel({ onGenerate, generating }) {
  const [prompt,    setPrompt]    = useState('');
  const [negPrompt, setNegPrompt] = useState('blurry, low quality, ugly, deformed, watermark');
  const [advanced,  setAdvanced]  = useState(false);
  const [settings,  setSettings]  = useState({ width: 1024, height: 1024, guidanceScale: 7.5, numInferenceSteps: 25 });

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!prompt.trim() || generating) return;
    onGenerate({ prompt: prompt.trim(), negativePrompt: negPrompt.trim() || undefined, ...settings });
  };

  const useExample = (p) => setPrompt(p);

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="p-6 border-b border-white/5">
        <h2 className="text-xl font-bold text-white flex items-center gap-2">
          <Wand2 className="w-5 h-5 text-violet-400" />
          Generate Image
        </h2>
        <p className="text-slate-500 text-sm mt-1">Describe what you want to create</p>
      </div>

      <div className="flex-1 overflow-y-auto p-6 space-y-5">
        {/* Main prompt */}
        <div>
          <label className="label">
            Prompt <span className="text-violet-400">*</span>
          </label>
          <textarea
            value={prompt}
            onChange={(e) => setPrompt(e.target.value)}
            placeholder="A majestic dragon soaring over snowy mountains at sunset, cinematic, 8K..."
            rows={5}
            className="input-field resize-none leading-relaxed"
            maxLength={1000}
          />
          <div className="flex justify-between items-center mt-1.5">
            <span className="text-xs text-slate-600">{prompt.length}/1000</span>
          </div>
        </div>

        {/* Example prompts */}
        <div>
          <p className="text-xs font-medium text-slate-500 mb-2">✨ Try an example:</p>
          <div className="flex flex-wrap gap-2">
            {EXAMPLE_PROMPTS.map((p, i) => (
              <button
                key={i}
                onClick={() => useExample(p)}
                className="text-xs px-3 py-1.5 rounded-full bg-dark-600 border border-dark-400 text-slate-400 hover:border-violet-500 hover:text-violet-400 transition-all duration-200 text-left"
              >
                {p.substring(0, 40)}...
              </button>
            ))}
          </div>
        </div>

        {/* Advanced settings toggle */}
        <button
          onClick={() => setAdvanced(a => !a)}
          className="flex items-center gap-2 text-sm text-slate-400 hover:text-slate-300 transition-colors"
        >
          {advanced ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
          Advanced Settings
        </button>

        {advanced && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="space-y-4 p-4 rounded-xl bg-dark-700/50 border border-dark-400"
          >
            {/* Negative Prompt */}
            <div>
              <label className="label">Negative Prompt</label>
              <textarea value={negPrompt} onChange={(e) => setNegPrompt(e.target.value)}
                rows={2} className="input-field resize-none text-sm"
                placeholder="Things to exclude from the image..." />
            </div>

            {/* Size */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="label">Width: <span className="text-violet-400">{settings.width}px</span></label>
                <input type="range" min={256} max={1024} step={64}
                  value={settings.width}
                  onChange={(e) => setSettings(s => ({ ...s, width: +e.target.value }))}
                  className="w-full accent-violet-500" />
              </div>
              <div>
                <label className="label">Height: <span className="text-violet-400">{settings.height}px</span></label>
                <input type="range" min={256} max={1024} step={64}
                  value={settings.height}
                  onChange={(e) => setSettings(s => ({ ...s, height: +e.target.value }))}
                  className="w-full accent-violet-500" />
              </div>
            </div>

            {/* Guidance Scale */}
            <div>
              <label className="label">
                Guidance Scale: <span className="text-violet-400">{settings.guidanceScale}</span>
                <span className="text-slate-600 ml-2">(how closely to follow the prompt)</span>
              </label>
              <input type="range" min={1} max={20} step={0.5}
                value={settings.guidanceScale}
                onChange={(e) => setSettings(s => ({ ...s, guidanceScale: +e.target.value }))}
                className="w-full accent-violet-500" />
            </div>

            {/* Inference Steps */}
            <div>
              <label className="label">
                Quality Steps: <span className="text-violet-400">{settings.numInferenceSteps}</span>
                <span className="text-slate-600 ml-2">(more = better quality, slower)</span>
              </label>
              <input type="range" min={10} max={50} step={5}
                value={settings.numInferenceSteps}
                onChange={(e) => setSettings(s => ({ ...s, numInferenceSteps: +e.target.value }))}
                className="w-full accent-violet-500" />
            </div>
          </motion.div>
        )}
      </div>

      {/* Generate button */}
      <div className="p-6 border-t border-white/5">
        <button
          onClick={handleSubmit}
          disabled={!prompt.trim() || generating}
          className="btn-primary w-full py-4 text-base flex items-center justify-center gap-3"
        >
          {generating ? (
            <>
              <Loader2 size={20} className="animate-spin" />
              <span>Generating... (this may take 30-120s)</span>
            </>
          ) : (
            <>
              <Sparkles size={20} />
              <span>Generate Image</span>
            </>
          )}
        </button>
        {!generating && (
          <p className="text-center text-xs text-slate-600 mt-2">Uses 1 credit per generation</p>
        )}
      </div>
    </div>
  );
}
