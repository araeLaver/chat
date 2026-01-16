import { Snackbar, Alert, Button, IconButton } from '@mui/material';
import { Close } from '@mui/icons-material';
import { useNotificationStore } from '../../stores/notificationStore';

export function NotificationSnackbar() {
  const { notifications, removeNotification } = useNotificationStore();

  // 가장 최근 알림만 표시
  const notification = notifications[0];

  if (!notification) {
    return null;
  }

  const handleClose = (_event?: React.SyntheticEvent | Event, reason?: string) => {
    if (reason === 'clickaway') {
      return;
    }
    removeNotification(notification.id);
  };

  return (
    <Snackbar
      open={true}
      autoHideDuration={notification.duration}
      onClose={handleClose}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
    >
      <Alert
        onClose={handleClose}
        severity={notification.type}
        variant="filled"
        sx={{ width: '100%', alignItems: 'center' }}
        action={
          <>
            {notification.action && (
              <Button
                color="inherit"
                size="small"
                onClick={() => {
                  notification.action?.onClick();
                  removeNotification(notification.id);
                }}
              >
                {notification.action.label}
              </Button>
            )}
            <IconButton
              size="small"
              aria-label="close"
              color="inherit"
              onClick={handleClose}
            >
              <Close fontSize="small" />
            </IconButton>
          </>
        }
      >
        {notification.message}
      </Alert>
    </Snackbar>
  );
}
