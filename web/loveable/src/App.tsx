import {ReactKeycloakProvider} from '@react-keycloak/web';
import {BrowserRouter, Route, Routes} from 'react-router-dom';
import {Toaster as Sonner} from '@/components/ui/sonner';
import keycloak from '@/lib/keycloak';
import KeycloakProtected from '@/components/KeycloakProtected';
import AppShell from '@/components/AppShell';
import TablesPage from '@/pages/TablesPage';
import TableDetailPage from '@/pages/TableDetailPage';
import NotFound from '@/pages/NotFound';

const App = () => (
    <ReactKeycloakProvider authClient={keycloak} initOptions={{onLoad: 'login-required', pkceMethod: 'S256'}}>
        <Sonner/>
        <BrowserRouter>
            <KeycloakProtected>
                <AppShell>
                    <Routes>
                        <Route path="/" element={<TablesPage/>}/>
                        <Route path="/tables/:tableId" element={<TableDetailPage/>}/>
                        <Route path="*" element={<NotFound/>}/>
                    </Routes>
                </AppShell>
            </KeycloakProtected>
        </BrowserRouter>
    </ReactKeycloakProvider>
);

export default App;
