import api from './axios';

export const generateImage = (data)  => api.post('/api/images/generate', data, { timeout: 180000 });
export const getMyImages   = ()      => api.get('/api/images/my-images');
export const deleteImage   = (id)    => api.delete(`/api/images/${id}`);
