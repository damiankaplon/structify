import {StrictMode} from 'react'
import {createRoot} from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import {ReactKeycloakProvider} from "@react-keycloak/web";
import keycloak from "./keycloak.ts";
import KeycloakProtected from "./security/KeycloakProtected.tsx";
import {Box, CircularProgress} from "@mui/material";

createRoot(document.getElementById('root')!).render(
    <ReactKeycloakProvider authClient={keycloak}>
        <StrictMode>
            <KeycloakProtected loadingComponent={
                <Box sx={{display: 'flex', width: '100%', height: '90dvh', justifyContent: 'center', alignItems: 'center'}}>
                    <CircularProgress/>
                </Box>
            }
            >
                <App/>
            </KeycloakProtected>
        </StrictMode>
    </ReactKeycloakProvider>
)
