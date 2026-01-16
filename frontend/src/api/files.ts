import apiClient from './axios';

export interface UploadedFile {
  id: number;
  filename: string;
  originalName: string;
  contentType: string;
  size: number;
  url: string;
  uploadedAt: string;
}

export const filesApi = {
  // 파일 업로드 (채팅방용)
  upload: async (file: File, roomId: string): Promise<UploadedFile> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('roomId', roomId);

    const response = await apiClient.post<UploadedFile>('/api/files/upload/room', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  // DM용 파일 업로드
  uploadForDM: async (file: File, conversationId: number): Promise<UploadedFile> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('conversationId', conversationId.toString());

    const response = await apiClient.post<UploadedFile>('/api/files/upload/dm', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  // 다중 파일 업로드
  uploadMultiple: async (files: File[], roomId?: string): Promise<UploadedFile[]> => {
    const formData = new FormData();
    files.forEach((file) => {
      formData.append('files', file);
    });
    if (roomId) {
      formData.append('roomId', roomId);
    }

    const response = await apiClient.post<UploadedFile[]>('/api/files/upload/multiple', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  // 파일 다운로드 URL
  getDownloadUrl: (filename: string): string => {
    return `/api/files/download/${filename}`;
  },

  // 파일 삭제
  delete: async (fileId: number): Promise<void> => {
    await apiClient.delete(`/api/files/${fileId}`);
  },

  // 이미지 썸네일 URL
  getThumbnailUrl: (filename: string, width = 200, height = 200): string => {
    return `/api/files/thumbnail/${filename}?w=${width}&h=${height}`;
  },

  // 파일 정보 조회
  getFileInfo: async (fileId: number): Promise<UploadedFile> => {
    const response = await apiClient.get<UploadedFile>(`/api/files/${fileId}`);
    return response.data;
  },
};
