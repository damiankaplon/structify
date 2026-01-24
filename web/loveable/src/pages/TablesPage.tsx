import React, {useEffect, useState} from 'react';
import {useQuery} from '@tanstack/react-query';
import MainLayout from '@/components/layout/MainLayout';
import CreateTableDialog from '@/components/tables/CreateTableDialog';
import TableCard from '@/components/tables/TableCard';
import {useApi} from '@/hooks/useApi';
import {tablesApi, Table} from '@/lib/api';
import {Loader2, Table2, FolderOpen} from 'lucide-react';

const TablesPage: React.FC = () => {
    const api = useApi();

    const {
        data: tables,
        isLoading,
        refetch,
    } = useQuery<Table[]>({
        queryKey: ['tables'],
        queryFn: () => tablesApi.getAll(api),
    });

    return (
        <MainLayout>
            <div className="space-y-6">
                <div className="flex items-center justify-between">
                    <div>
                        <h1 className="text-3xl font-bold text-foreground">Tables</h1>
                        <p className="text-muted-foreground mt-1">
                            Define table structures and extract data from PDF documents
                        </p>
                    </div>
                    <CreateTableDialog onTableCreated={() => refetch()}/>
                </div>

                {isLoading ? (
                    <div className="flex items-center justify-center py-12">
                        <Loader2 className="h-8 w-8 animate-spin text-primary"/>
                    </div>
                ) : tables && tables.length > 0 ? (
                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                        {tables.map((table) => (
                            <TableCard key={table.id} table={table}/>
                        ))}
                    </div>
                ) : (
                    <div className="flex flex-col items-center justify-center py-16 text-center">
                        <div className="rounded-full bg-muted p-4 mb-4">
                            <FolderOpen className="h-8 w-8 text-muted-foreground"/>
                        </div>
                        <h3 className="text-lg font-medium text-foreground">No tables yet</h3>
                        <p className="text-muted-foreground mt-1 max-w-sm">
                            Create your first table to define a data structure for extracting
                            information from PDF documents.
                        </p>
                    </div>
                )}
            </div>
        </MainLayout>
    );
};

export default TablesPage;
