import { Box } from '@mui/material';
import { Outlet } from 'react-router-dom';
import BottomTabBar from '../components/navigation/BottomTabBar';

export default function MainLayout() {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <Box sx={{ flex: 1, overflow: 'auto', pb: '56px' }}>
        <Outlet />
      </Box>
      <BottomTabBar />
    </Box>
  );
}
