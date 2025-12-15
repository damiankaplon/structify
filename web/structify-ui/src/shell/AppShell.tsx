import React from "react";
import AppBar from "@mui/material/AppBar";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import IconButton from "@mui/material/IconButton";
import MenuIcon from "@mui/icons-material/Menu";
import Drawer from "@mui/material/Drawer";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemIcon from "@mui/material/ListItemIcon";
import ListItemText from "@mui/material/ListItemText";
import Divider from "@mui/material/Divider";
import Box from "@mui/material/Box";
import TableChartIcon from "@mui/icons-material/TableChart";
import {Outlet, useLocation, useNavigate} from "react-router-dom";

export type NavigationTab = {
    title: string;
    route: string;
    icon?: React.ReactNode;
};

type AppShellProps = {
    title?: string;
    onTabChange?: (tab: NavigationTab) => void;
    children?: React.ReactNode;
};

export default function AppShell(
    {
        title = "Structify",
        onTabChange,
    }: AppShellProps
) {
    const [drawerOpen, setDrawerOpen] = React.useState(false);
    const NAV_TABS: NavigationTab[] = React.useMemo(() => ([
        {title: "Tables", route: "/tables", icon: <TableChartIcon/>},
    ]), []);

    const [tab, setTab] = React.useState<NavigationTab>(NAV_TABS[0]);
    const navigate = useNavigate();
    const location = useLocation();

    const handleTabChange = (value: NavigationTab) => {
        setTab(value);
        onTabChange?.(value);
        navigate(value.route);
    };

    React.useEffect(() => {
        const match = NAV_TABS.find(t => location.pathname.startsWith(t.route));
        if (match && match.route !== tab.route) {
            setTab(match);
        }
    }, [location.pathname, NAV_TABS, tab.route]);

    return (
        <Box sx={{display: "flex"}}>
            <AppBar position="fixed" color="primary" sx={{zIndex: (t) => t.zIndex.drawer + 1}}>
                <Toolbar>
                    <IconButton
                        color="inherit"
                        aria-label="open drawer"
                        edge="start"
                        onClick={() => setDrawerOpen((open) => !open)}
                        sx={{mr: 2}}
                    >
                        <MenuIcon/>
                    </IconButton>
                    <Typography variant="h6" noWrap component="div" sx={{mr: 3}}>
                        {title}
                    </Typography>
                </Toolbar>
            </AppBar>

            <Drawer anchor="left" open={drawerOpen} onClose={() => setDrawerOpen(false)}>
                <Box
                    role="presentation"
                    sx={{width: 260}}
                    onClick={() => setDrawerOpen(false)}
                    onKeyDown={() => setDrawerOpen(false)}
                >
                    <Typography variant="h6" sx={{px: 2, py: 2}}>
                        {title}
                    </Typography>
                    <Divider/>
                    <List>
                        {NAV_TABS.map((item) => (
                            <ListItem key={item.route} disablePadding>
                                <ListItemButton
                                    selected={tab.route === item.route}
                                    onClick={() => handleTabChange(item)}
                                >
                                    <ListItemIcon>{item.icon}</ListItemIcon>
                                    <ListItemText primary={item.title}/>
                                </ListItemButton>
                            </ListItem>
                        ))}
                    </List>
                </Box>
            </Drawer>

            <Box component="main" sx={{flexGrow: 1, p: 3}}>
                {/* spacer for fixed AppBar */}
                <Toolbar/>
                {/* Nested route content */}
                <Outlet/>
            </Box>
        </Box>
    );
}
