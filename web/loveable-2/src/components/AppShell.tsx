import React from 'react';
import {Link, useLocation} from 'react-router-dom';
import {useKeycloak} from '@react-keycloak/web';

const AppShell: React.FC<{ children: React.ReactNode }> = ({children}) => {
    const {keycloak} = useKeycloak();
    const location = useLocation();

    return (
        <div className="min-h-screen bg-background">
            <header className="sticky top-0 z-30 border-b border-border bg-card/80 backdrop-blur-md">
                <div className="mx-auto flex h-14 max-w-6xl items-center justify-between px-4">
                    <Link to="/" className="flex items-center gap-2">
                        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary">
                            <span className="text-sm font-bold text-primary-foreground">S</span>
                        </div>
                        <span className="text-lg font-semibold text-foreground">Structify</span>
                    </Link>

                    <nav className="flex items-center gap-6">
                        <Link
                            to="/"
                            className={`text-sm font-medium transition-colors hover:text-primary ${
                                location.pathname === '/' ? 'text-primary' : 'text-muted-foreground'
                            }`}
                        >
                            Tables
                        </Link>
                        <button
                            onClick={() => keycloak.logout()}
                            className="rounded-lg bg-secondary px-3 py-1.5 text-xs font-medium text-secondary-foreground transition-colors hover:bg-secondary/80"
                        >
                            Sign out
                        </button>
                    </nav>
                </div>
            </header>

            <main className="mx-auto max-w-6xl px-4 py-8">{children}</main>
        </div>
    );
};

export default AppShell;
