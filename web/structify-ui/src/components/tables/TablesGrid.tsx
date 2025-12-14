import React, {useEffect, useState} from 'react';
import {Box, Card, CardActionArea, CardContent, CircularProgress, Grid, Typography} from '@mui/material';
import {Table} from "./Table.ts";
import {TablesApi} from "./TablesApi.ts";
import {useNavigate} from "react-router-dom";

interface TablesGridProps {
    tablesApi: TablesApi;
}

const TablesGrid: React.FC<TablesGridProps> = ({tablesApi}) => {
    const [tables, setTables] = useState<Table[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchTables = async () => {
            try {
                const data = await tablesApi.fetchTables();
                setTables(data);
            } catch (err) {
                setError(err instanceof Error ? err.message : 'An unknown error occurred');
            } finally {
                setLoading(false);
            }
        };
        void fetchTables();
    }, [tablesApi]);

    if (loading) {
        return <Box sx={{display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%'}}><CircularProgress/></Box>;
    }

    if (error) {
        return <Typography color="error">Error fetching tables: {error}</Typography>;
    }

    return (
        <Box sx={{p: 3}}>
            <Typography variant="h4" gutterBottom>
                Your Tables
            </Typography>
            {tables.length === 0 ? (
                <Typography>You don't have any tables yet.</Typography>
            ) : (
                <Grid container spacing={3}>
                    {tables.map((table) => (
                        <Grid item xs={12} sm={6} md={4} key={table.id}>
                            <Card>
                                <CardActionArea onClick={() => navigate(`/tables/${table.id}`)}>
                                    <CardContent>
                                        <Typography variant="h6">{table.name}</Typography>
                                    </CardContent>
                                </CardActionArea>
                            </Card>
                        </Grid>
                    ))}
                </Grid>
            )}
        </Box>
    );
};

export default TablesGrid;
