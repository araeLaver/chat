export type FileCategory = 'IMAGE' | 'VIDEO' | 'AUDIO' | 'DOCUMENT' | 'OTHER';

export interface FileUploadResponse {
  success: boolean;
  fileId: number;
  fileName: string;
  fileSize: number;
  fileType: string;
  category: FileCategory;
  hasThumbnail: boolean;
  message: string;
}

export interface FileMetadata {
  id: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  category: FileCategory;
  hasThumbnail: boolean;
  uploadedAt: string;
  uploaderId: number;
}
