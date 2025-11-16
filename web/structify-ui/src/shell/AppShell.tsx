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

type NavKey = "tables";

type AppShellProps = {
    title?: string;
    initialTab?: NavKey;
    onTabChange?: (tab: NavKey) => void;
    children?: React.ReactNode;
};

const drawerWidth = 260;

export default function AppShell(
    {
        title = "Structify",
        initialTab = "tables",
        onTabChange,
        children,
    }: AppShellProps
) {
    const [drawerOpen, setDrawerOpen] = React.useState(false);
    const [tab, setTab] = React.useState<NavKey>(initialTab);

    const handleTabChange = (value: NavKey) => {
        setTab(value);
        onTabChange?.(value);
    };

    const drawerItems: { key: NavKey; label: string; icon: React.ReactNode }[] = [
        {key: "tables", label: "Tables", icon: <TableChartIcon/>},
    ];

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
                    sx={{width: drawerWidth}}
                    onClick={() => setDrawerOpen(false)}
                    onKeyDown={() => setDrawerOpen(false)}
                >
                    <Typography variant="h6" sx={{px: 2, py: 2}}>
                        {title}
                    </Typography>
                    <Divider/>
                    <List>
                        {drawerItems.map((item) => (
                            <ListItem key={item.key} disablePadding>
                                <ListItemButton
                                    selected={tab === item.key}
                                    onClick={() => handleTabChange(item.key)}
                                >
                                    <ListItemIcon>{item.icon}</ListItemIcon>
                                    <ListItemText primary={item.label}/>
                                </ListItemButton>
                            </ListItem>
                        ))}
                    </List>
                </Box>
            </Drawer>

            <Box component="main" sx={{flexGrow: 1, p: 3}}>
                {/* spacer for fixed AppBar */}
                <Toolbar/>
                {children}
            </Box>
        </Box>
    );
}
