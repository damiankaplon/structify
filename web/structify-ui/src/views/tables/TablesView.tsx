import React, {useContext, useMemo, useState} from 'react';
import TablesGrid from '../../components/tables/TablesGrid';
import {TablesFetchApi} from '../../components/tables/TablesApi';
import {AuthContext} from '../../security/AuthContext';
import {Box, Button} from '@mui/material';
import CreateTableDialog, {CreateTableFormInput} from '../../components/tables/CreateTableDialog';
import {Table} from "../../components/tables/Table.ts";

const TablesView: React.FC = () => {
    const authContext = useContext(AuthContext);
    const tablesApi = useMemo(() => new TablesFetchApi(() => authContext.jwt!), [authContext.jwt]);
    const [dialogOpen, setDialogOpen] = useState(false);

    const onTableCreate = async ({name, description}: CreateTableFormInput): Promise<string | Error> => {
        try {
            return await tablesApi.createTable(name.trim(), description.trim());
        } catch (e) {
            return e instanceof Error ? e : new Error('Failed to create table');
        }
    };

    const fetchTables = async (): Promise<Table[] | Error> => {
        try {
            return await tablesApi.fetchTables();
        } catch (e) {
            return e instanceof Error ? e : new Error('Failed to fetch tables');
        }
    }

    return (
        <Box>
            <Box sx={{display: 'flex', justifyContent: 'flex-start', p: 3}}>
                <Button
                    variant="contained"
                    onClick={() => setDialogOpen(true)}>
                    New table
                </Button>
            </Box>
            <TablesGrid tablesProvider={fetchTables}/>
            <CreateTableDialog
                open={dialogOpen}
                onClose={() => setDialogOpen(false)}
                onSubmit={onTableCreate}
            />
        </Box>
    );
};

export default TablesView;
