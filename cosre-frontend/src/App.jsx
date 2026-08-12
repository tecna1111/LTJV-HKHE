<<<<<<< HEAD
import { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from './assets/vite.svg'
import heroImg from './assets/hero.png'
import './App.css'
import CreateProjectPage from "./features/lecturer/pages/CreateProjectPage";
<Route path="/lecturer/projects/new" element={<CreateProjectPage />} />

import CreateProjectPage from "./features/lecturer/pages/CreateProjectPage";
=======
import './App.css';
import AppRoutes from './routes/AppRoutes';
>>>>>>> 86347006fb67935a1c7facdfcce031cd858fafc2

function App() {
  return <AppRoutes />;
}

export default App;
