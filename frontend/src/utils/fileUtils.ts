import { FileCategory } from '../types/file';

export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

export function getFileCategory(mimeType: string): FileCategory {
  if (mimeType.startsWith('image/')) return 'IMAGE';
  if (mimeType.startsWith('video/')) return 'VIDEO';
  if (mimeType.startsWith('audio/')) return 'AUDIO';
  if (
    mimeType.includes('pdf') ||
    mimeType.includes('word') ||
    mimeType.includes('excel') ||
    mimeType.includes('document')
  )
    return 'DOCUMENT';
  return 'OTHER';
}

export function isImageFile(mimeType: string): boolean {
  return mimeType.startsWith('image/');
}

export function getFileIcon(category: FileCategory): string {
  switch (category) {
    case 'IMAGE': return '🖼️';
    case 'VIDEO': return '🎬';
    case 'AUDIO': return '🎵';
    case 'DOCUMENT': return '📄';
    default: return '📎';
  }
}
