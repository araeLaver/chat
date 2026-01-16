import React from 'react';
import { Box, Button, Typography } from '@mui/material';
import { ErrorOutline } from '@mui/icons-material';

interface ErrorBoundaryState {
  hasError: boolean;
  error?: Error;
}

interface ErrorBoundaryProps {
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

export class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: undefined });
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <Box
          display="flex"
          flexDirection="column"
          alignItems="center"
          justifyContent="center"
          minHeight="200px"
          gap={2}
          p={3}
        >
          <ErrorOutline sx={{ fontSize: 48, color: 'error.main' }} />
          <Typography variant="h6" color="error">
            오류가 발생했습니다
          </Typography>
          <Typography variant="body2" color="text.secondary" textAlign="center">
            {this.state.error?.message || '알 수 없는 오류가 발생했습니다.'}
          </Typography>
          <Button variant="contained" onClick={this.handleRetry}>
            다시 시도
          </Button>
        </Box>
      );
    }

    return this.props.children;
  }
}
