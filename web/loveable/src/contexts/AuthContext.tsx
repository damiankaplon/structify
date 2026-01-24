import React, {createContext, useContext} from 'react';

interface AuthContextType {
    jwt: string;
}

export const AuthContext = createContext<AuthContextType | null>(null);

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

interface AuthProviderProps {
    jwtProvider: () => string;
    children: React.ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({jwtProvider, children}) => {
    return (
        <AuthContext.Provider value={{jwt: jwtProvider()}}>
            {children}
        </AuthContext.Provider>
    );
};
