import React, {createContext, useCallback, useContext} from 'react';

interface AuthContextType {
    getToken: () => string;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const AuthProvider: React.FC<{
    jwtProvider: () => string;
    children: React.ReactNode;
}> = ({jwtProvider, children}) => {
    const getToken = useCallback(() => jwtProvider(), [jwtProvider]);
    return (
        <AuthContext.Provider value={{getToken}}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error('useAuth must be used within AuthProvider');
    return ctx;
};
