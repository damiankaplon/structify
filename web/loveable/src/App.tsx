import {Toaster} from "@/components/ui/toaster";
import {Toaster as Sonner} from "@/components/ui/sonner";
import {TooltipProvider} from "@/components/ui/tooltip";
import {QueryClient, QueryClientProvider} from "@tanstack/react-query";
import {BrowserRouter, Routes, Route} from "react-router-dom";
import {ReactKeycloakProvider} from "@react-keycloak/web";
import keycloak from "@/lib/keycloak";
import KeycloakProtected from "@/components/KeycloakProtected";
import TablesPage from "./pages/TablesPage";
import TableDetailPage from "./pages/TableDetailPage";
import NotFound from "./pages/NotFound";

const queryClient = new QueryClient();

const App = () => (
    <ReactKeycloakProvider authClient={keycloak}>
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>
                <Toaster/>
                <Sonner/>
                <KeycloakProtected>
                    <BrowserRouter>
                        <Routes>
                            <Route path="/" element={<TablesPage/>}/>
                            <Route path="/tables/:tableId" element={<TableDetailPage/>}/>
                            <Route path="*" element={<NotFound/>}/>
                        </Routes>
                    </BrowserRouter>
                </KeycloakProtected>
            </TooltipProvider>
        </QueryClientProvider>
    </ReactKeycloakProvider>
);

export default App;
