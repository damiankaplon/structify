import './App.css';
import '@fontsource/roboto/300.css';
import '@fontsource/roboto/400.css';
import '@fontsource/roboto/500.css';
import '@fontsource/roboto/700.css';
import AppShell from './shell/AppShell';

export default function App() {
    return (
        <AppShell
            title="Structify"
            onTabChange={(tab) => {
                // Hook your router here if needed, e.g. navigate(`/${tab}`)
                console.log('Tab changed:', tab);
            }}
        >
            <div>Welcome to Structify</div>
        </AppShell>
    );
}
