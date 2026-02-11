import apiClient from './client';
import { FileUploadResponse, FileMetadata } from '../types/file';

export const filesApi = {
  uploadDM(file: File, conversationId: string) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('conversationId', conversationId);
    return apiClient.post<FileUploadResponse>('/files/upload/dm', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  uploadRoom(file: File, roomId: number) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('roomId', String(roomId));
    return apiClient.post<FileUploadResponse>('/files/upload/room', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  getDownloadUrl(fileId: number) {
    return `/api/files/download/${fileId}`;
  },

  getThumbnailUrl(fileId: number) {
    return `/api/files/thumbnail/${fileId}`;
  },

  getFileInfo(fileId: number) {
    return apiClient.get<FileMetadata>(`/files/info/${fileId}`);
  },

  getConversationFiles(conversationId: string) {
    return apiClient.get<FileMetadata[]>(`/files/conversation/${conversationId}`);
  },

  getRoomFiles(roomId: number) {
    return apiClient.get<FileMetadata[]>(`/files/room/${roomId}`);
  },

  deleteFile(fileId: number) {
    return apiClient.delete(`/files/${fileId}`);
  },
};
