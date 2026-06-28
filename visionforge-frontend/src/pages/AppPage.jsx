import { useState, useEffect, useCallback } from 'react';
import Sidebar from '../components/Sidebar';
import GeneratePanel from '../components/GeneratePanel';
import ImageDisplay from '../components/ImageDisplay';
import { getMyImages, generateImage as generateImageApi, deleteImage as deleteImageApi } from '../api/images';
import { useAuth } from '../context/AuthContext';

export default function AppPage() {
  const { user, login } = useAuth();
  const [images, setImages] = useState([]);
  const [selectedImage, setSelectedImage] = useState(null);
  const [generating, setGenerating] = useState(false);
  const [generatingPrompt, setGeneratingPrompt] = useState('');
  const [loadingHistory, setLoadingHistory] = useState(true);

  // Fetch image history on load
  const fetchHistory = useCallback(async () => {
    try {
      setLoadingHistory(true);
      const res = await getMyImages();
      setImages(res.data);
      if (res.data.length > 0 && !selectedImage) {
        setSelectedImage(res.data[0]);
      }
    } catch (err) {
      console.error('Error fetching image history:', err);
    } finally {
      setLoadingHistory(false);
    }
  }, [selectedImage]);

  useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);

  const handleGenerate = async (params) => {
    setGenerating(true);
    setGeneratingPrompt(params.prompt);
    setSelectedImage(null);
    try {
      const res = await generateImageApi(params);
      
      // Update image history
      setImages(prev => [res.data, ...prev]);
      setSelectedImage(res.data);

      // Decrement credits in the context user object
      if (user) {
        const updatedUser = { ...user, credits: Math.max(0, user.credits - 1) };
        login({ accessToken: localStorage.getItem('vf_token'), user: updatedUser });
      }
    } catch (err) {
      console.error('Error generating image:', err);
      // In case of a failed status response recorded in the DB, reload history
      fetchHistory();
    } finally {
      setGenerating(false);
      setGeneratingPrompt('');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this image?')) return;
    try {
      await deleteImageApi(id);
      setImages(prev => prev.filter(img => img.id !== id));
      if (selectedImage?.id === id) {
        setSelectedImage(null);
      }
    } catch (err) {
      console.error('Error deleting image:', err);
      alert('Failed to delete image');
    }
  };

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-dark-900 text-white">
      {/* Sidebar with Image History */}
      <Sidebar
        images={images}
        selectedId={selectedImage?.id}
        onSelect={setSelectedImage}
        onDelete={handleDelete}
        loading={loadingHistory}
      />

      {/* Main Workspace Layout */}
      <main className="flex-1 flex overflow-hidden">
        {/* Left Side: Generation parameters */}
        <section className="w-[450px] border-r border-white/5 bg-dark-800/20 backdrop-blur-md h-full shrink-0">
          <GeneratePanel onGenerate={handleGenerate} generating={generating} />
        </section>

        {/* Right Side: Image result viewer */}
        <section className="flex-1 bg-dark-900/40 backdrop-blur-lg h-full">
          <ImageDisplay
            image={selectedImage}
            generating={generating}
            generatingPrompt={generatingPrompt}
            onDelete={handleDelete}
          />
        </section>
      </main>
    </div>
  );
}
