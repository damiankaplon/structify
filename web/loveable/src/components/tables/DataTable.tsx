import React from 'react';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow,} from '@/components/ui/table';
import {Column, getLeafColumns, Row} from '@/lib/api';
import {Badge} from '@/components/ui/badge';

interface DataTableProps {
    columns: Column[];
    rows: Row[];
}

const DataTable: React.FC<DataTableProps> = ({columns, rows}) => {
    const leafColumns = getLeafColumns(columns);

    const getCellValue = (row: Row, columnId: string): string => {
        const cell = row.cells.find((c) => c.columnDefinitionId === columnId);
        return cell?.value || '-';
    };

    if (rows.length === 0) {
        return (
            <div className="text-center py-12 text-muted-foreground">
                <p>No data yet. Upload a PDF or add rows manually.</p>
            </div>
        );
    }

    return (
        <div className="rounded-md border overflow-x-auto">
            <Table>
                <TableHeader>
                    <TableRow>
                        {leafColumns.map((column) => (
                            <TableHead key={column.id}>
                                <div className="flex items-center gap-2">
                                    {column.name}
                                    {column.optional && (
                                        <Badge variant="secondary" className="text-xs">
                                            Optional
                                        </Badge>
                                    )}
                                </div>
                            </TableHead>
                        ))}
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {rows.map((row) => (
                        <TableRow key={row.id}>
                            {leafColumns.map((column) => (
                                <TableCell key={column.id}>
                                    {getCellValue(row, column.id)}
                                </TableCell>
                            ))}
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </div>
    );
};

export default DataTable;
