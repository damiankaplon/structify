import React from 'react';
import {useKeycloak} from '@react-keycloak/web';
import {Button} from '@/components/ui/button';
import {LogOut, Table2} from 'lucide-react';

const Header: React.FC = () => {
    const {keycloak} = useKeycloak();

    const handleLogout = () => {
        keycloak.logout();
    };

    return (
        <header className="border-b border-border bg-card">
            <div className="container flex h-16 items-center justify-between">
                <div className="flex items-center gap-2">
                    <Table2 className="h-6 w-6 text-primary"/>
                    <span className="text-xl font-semibold text-foreground">Structify</span>
                </div>
                <div className="flex items-center gap-4">
          <span className="text-sm text-muted-foreground">
            {keycloak.tokenParsed?.preferred_username || keycloak.tokenParsed?.email}
          </span>
                    <Button variant="ghost" size="sm" onClick={handleLogout}>
                        <LogOut className="mr-2 h-4 w-4"/>
                        Logout
                    </Button>
                </div>
            </div>
        </header>
    );
};

export default Header;
