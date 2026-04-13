import {ReactKeycloakProvider} from '@react-keycloak/web';
import {BrowserRouter, Route, Routes} from 'react-router-dom';
import {Toaster as Sonner} from '@/components/ui/sonner';
import keycloak from '@/lib/keycloak';
import KeycloakProtected from '@/components/KeycloakProtected';
import AppShell from '@/components/AppShell';
import TablesPage from '@/pages/TablesPage';
import TableDetailPage from '@/pages/TableDetailPage';
import NotFound from '@/pages/NotFound';
import LandingPage from '@/pages/LandingPage';

const App = () => (
  <ReactKeycloakProvider authClient={keycloak} initOptions={{onLoad: 'check-sso', pkceMethod: 'S256'}}>
    <Sonner/>
    <BrowserRouter>
      <Routes>
        {/* Public landing page — no auth required */}
        <Route path="/" element={<LandingPage/>}/>

        {/* Authenticated app routes */}
        <Route
          path="/app/*"
          element={
            <KeycloakProtected>
              <AppShell>
                <Routes>
                  <Route path="/" element={<TablesPage/>}/>
                  <Route path="/tables/:tableId" element={<TableDetailPage/>}/>
                  <Route path="*" element={<NotFound/>}/>
                </Routes>
              </AppShell>
            </KeycloakProtected>
          }
        />

        <Route path="*" element={<NotFound/>}/>
      </Routes>
    </BrowserRouter>
  </ReactKeycloakProvider>
);

export default App;
