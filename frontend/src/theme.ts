import { createTheme, type ThemeOptions } from '@mui/material/styles';

// KakaoTalk 스타일 색상
const kakaoColors = {
  yellow: '#FEE500',        // 카카오톡 메인 노란색
  yellowDark: '#F5DC00',    // 어두운 노란색
  yellowLight: '#FFF9C4',   // 밝은 노란색
  brown: '#3C1E1E',         // 카카오 브라운
  chatBubbleOwn: '#FEE500', // 내 메시지 배경
  chatBubbleOther: '#FFFFFF', // 상대 메시지 배경 (라이트모드)
  chatBubbleOtherDark: '#2D2D2D', // 상대 메시지 배경 (다크모드)
  chatBackground: '#B2C7D9', // 채팅방 배경 (라이트모드)
  chatBackgroundDark: '#1A1A1A', // 채팅방 배경 (다크모드)
};

const baseTheme: ThemeOptions = {
  typography: {
    fontFamily: [
      '-apple-system',
      'BlinkMacSystemFont',
      '"Segoe UI"',
      'Roboto',
      '"Noto Sans KR"',
      '"Helvetica Neue"',
      'Arial',
      'sans-serif',
    ].join(','),
    h1: {
      fontSize: '2.5rem',
      fontWeight: 600,
    },
    h2: {
      fontSize: '2rem',
      fontWeight: 600,
    },
    h3: {
      fontSize: '1.75rem',
      fontWeight: 600,
    },
    h4: {
      fontSize: '1.5rem',
      fontWeight: 500,
    },
    h5: {
      fontSize: '1.25rem',
      fontWeight: 500,
    },
    h6: {
      fontSize: '1rem',
      fontWeight: 500,
    },
  },
  shape: {
    borderRadius: 12,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          borderRadius: 8,
          fontWeight: 500,
        },
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            borderRadius: 8,
          },
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 16,
          boxShadow: '0 2px 12px rgba(0,0,0,0.08)',
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        rounded: {
          borderRadius: 12,
        },
      },
    },
    MuiAvatar: {
      styleOverrides: {
        root: {
          fontSize: '1rem',
        },
      },
    },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          margin: '2px 8px',
          '&.Mui-selected': {
            backgroundColor: 'rgba(25, 118, 210, 0.12)',
          },
        },
      },
    },
  },
};

export const lightTheme = createTheme({
  ...baseTheme,
  palette: {
    mode: 'light',
    primary: {
      main: kakaoColors.yellow,
      light: kakaoColors.yellowLight,
      dark: kakaoColors.yellowDark,
      contrastText: kakaoColors.brown,
    },
    secondary: {
      main: '#3C1E1E',
      light: '#6D4C41',
      dark: '#1B0C0C',
    },
    background: {
      default: '#f5f5f5',
      paper: '#ffffff',
    },
    text: {
      primary: '#212121',
      secondary: '#757575',
    },
    divider: 'rgba(0, 0, 0, 0.08)',
    // 커스텀 색상 (채팅 관련)
    chat: {
      bubbleOwn: kakaoColors.chatBubbleOwn,
      bubbleOther: kakaoColors.chatBubbleOther,
      background: kakaoColors.chatBackground,
    },
  } as any,
});

export const darkTheme = createTheme({
  ...baseTheme,
  palette: {
    mode: 'dark',
    primary: {
      main: kakaoColors.yellow,
      light: kakaoColors.yellowLight,
      dark: kakaoColors.yellowDark,
      contrastText: kakaoColors.brown,
    },
    secondary: {
      main: '#A1887F',
      light: '#D7CCC8',
      dark: '#6D4C41',
    },
    background: {
      default: '#121212',
      paper: '#1e1e1e',
    },
    text: {
      primary: '#ffffff',
      secondary: '#b0b0b0',
    },
    divider: 'rgba(255, 255, 255, 0.08)',
    // 커스텀 색상 (채팅 관련)
    chat: {
      bubbleOwn: kakaoColors.chatBubbleOwn,
      bubbleOther: kakaoColors.chatBubbleOtherDark,
      background: kakaoColors.chatBackgroundDark,
    },
  } as any,
});

export type ThemeMode = 'light' | 'dark';

// 커스텀 팔레트 타입 확장
declare module '@mui/material/styles' {
  interface Palette {
    chat: {
      bubbleOwn: string;
      bubbleOther: string;
      background: string;
    };
  }
  interface PaletteOptions {
    chat?: {
      bubbleOwn?: string;
      bubbleOther?: string;
      background?: string;
    };
  }
}

// 테마 색상 내보내기
export { kakaoColors };
