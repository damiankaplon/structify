import React, {useEffect, useState} from 'react';
import {Box, CircularProgress, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography} from '@mui/material';
import {Row} from './Row.ts';
import {VersionReadModel} from '../tables/TablesApi';

interface RowsGridProps {
    rowsProvider: () => Promise<Row[] | Error>;
    versionProvider: () => Promise<VersionReadModel | null | Error>;
}

const RowsGrid: React.FC<RowsGridProps> = ({rowsProvider, versionProvider}) => {
    const [rows, setRows] = useState<Row[]>([]);
    const [version, setVersion] = useState<VersionReadModel | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchData = async () => {
            const [rowsResult, versionResult] = await Promise.all([
                rowsProvider(),
                versionProvider()
            ]);

            if (rowsResult instanceof Error) {
                setError(rowsResult.message);
            } else {
                setRows(rowsResult);
            }

            if (versionResult instanceof Error) {
                setError(versionResult.message);
            } else {
                setVersion(versionResult);
            }

            setLoading(false);
        };
        void fetchData();
    }, [rowsProvider, versionProvider]);

    if (loading) {
        return <Box sx={{display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%'}}><CircularProgress/></Box>;
    }

    if (error) {
        return <Typography color="error">Error fetching rows: {error}</Typography>;
    }

    if (!version) {
        return (
            <Typography>No table version defined yet. Please create a version first.</Typography>
        );
    }

    return (
        <>
            {rows.length === 0 ? (
                <Typography>No rows in this table yet.</Typography>
            ) : (
                <TableContainer component={Paper}>
                    <Table>
                        <TableHead>
                            <TableRow>
                                {version.columns.map((col) => (
                                    <TableCell key={col.id}>{col.name}</TableCell>
                                ))}
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {rows.map((row) => {
                                // Create a map of columnId -> cell value for this row
                                const cellMap = new Map(row.cells.map(cell => [cell.columnId, cell.value]));

                                return (
                                    <TableRow key={row.id}>
                                        {version.columns.map((col) => (
                                            <TableCell key={col.id}>
                                                {cellMap.get(col.id) ?? '-'}
                                            </TableCell>
                                        ))}
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                </TableContainer>
            )}
        </>
    );
};

export default RowsGrid;
