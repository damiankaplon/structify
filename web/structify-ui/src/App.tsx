import './App.css';
import '@fontsource/roboto/300.css';
import '@fontsource/roboto/400.css';
import '@fontsource/roboto/500.css';
import '@fontsource/roboto/700.css';
import AppShell from './shell/AppShell';
import {BrowserRouter, Navigate, Route, Routes} from 'react-router-dom';
import TablesView from "./views/tables/TablesView";

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                {/* Default redirect to tables */}
                <Route path="/" element={<AppShell title="Structify"/>}>
                    <Route index element={<Navigate to="/tables" replace/>}/>
                    <Route path="tables" element={<TablesView/>}/>
                </Route>
                {/* Fallback */}
                <Route path="*" element={<Navigate to="/tables" replace/>}/>
            </Routes>
        </BrowserRouter>
    );
}
