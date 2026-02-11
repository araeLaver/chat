import { Box, Container } from '@mui/material';
import { Outlet } from 'react-router-dom';

export default function AuthLayout() {
  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        background: 'linear-gradient(180deg, #FEE500 0%, #FFF9C4 100%)',
      }}
    >
      <Container maxWidth="xs">
        <Outlet />
      </Container>
    </Box>
  );
}
