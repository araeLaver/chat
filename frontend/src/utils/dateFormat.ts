import { format, isToday, isYesterday, parseISO } from 'date-fns';
import { ko } from 'date-fns/locale';

export function formatMessageTime(dateStr: string): string {
  const date = typeof dateStr === 'string' ? parseISO(dateStr) : dateStr;
  return format(date, 'a h:mm', { locale: ko });
}

export function formatChatListTime(dateStr: string): string {
  if (!dateStr) return '';
  const date = typeof dateStr === 'string' ? parseISO(dateStr) : dateStr;
  if (isToday(date)) {
    return format(date, 'a h:mm', { locale: ko });
  }
  if (isYesterday(date)) {
    return '어제';
  }
  return format(date, 'M월 d일', { locale: ko });
}

export function formatDateDivider(dateStr: string): string {
  const date = typeof dateStr === 'string' ? parseISO(dateStr) : dateStr;
  if (isToday(date)) return '오늘';
  if (isYesterday(date)) return '어제';
  return format(date, 'yyyy년 M월 d일 EEEE', { locale: ko });
}
