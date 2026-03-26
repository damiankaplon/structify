import React from 'react';
import {useKeycloak} from '@react-keycloak/web';
import {AuthProvider} from '@/lib/auth';
import LoadingScreen from './LoadingScreen';

const KeycloakProtected: React.FC<{ children: React.ReactNode }> = ({children}) => {
    const {keycloak, initialized} = useKeycloak();

    if (!initialized) return <LoadingScreen/>;

    if (!keycloak.authenticated) {
        keycloak.login();
        return <LoadingScreen/>;
    }

    return (
        <AuthProvider jwtProvider={() => keycloak.token!}>
            {children}
        </AuthProvider>
    );
};

export default KeycloakProtected;
