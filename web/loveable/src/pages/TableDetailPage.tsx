import React, {useState} from 'react';
import {useParams, Link} from 'react-router-dom';
import {useQuery} from '@tanstack/react-query';
import MainLayout from '@/components/layout/MainLayout';
import EditColumnsDialog from '@/components/tables/EditColumnsDialog';
import CreateRowDialog from '@/components/tables/CreateRowDialog';
import PdfUpload from '@/components/tables/PdfUpload';
import DataTable from '@/components/tables/DataTable';
import {Button} from '@/components/ui/button';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {Badge} from '@/components/ui/badge';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {useApi} from '@/hooks/useApi';
import {tablesApi, TableVersion, Row} from '@/lib/api';
import {ArrowLeft, Loader2, Columns3, Settings, FileSpreadsheet, AlertCircle} from 'lucide-react';
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';

const TableDetailPage: React.FC = () => {
    const {tableId} = useParams<{ tableId: string }>();
    const api = useApi();
    const [isEditColumnsOpen, setIsEditColumnsOpen] = useState(false);

    const {
        data: version,
        isLoading: isVersionLoading,
        refetch: refetchVersion,
    } = useQuery<TableVersion | null>({
        queryKey: ['tableVersion', tableId],
        queryFn: () => tablesApi.getCurrentVersion(api, tableId!),
        enabled: !!tableId,
    });

    const {
        data: rows,
        isLoading: isRowsLoading,
        refetch: refetchRows,
    } = useQuery<Row[]>({
        queryKey: ['tableRows', tableId],
        queryFn: () => tablesApi.getRows(api, tableId!),
        enabled: !!tableId && !!version,
    });

    const handleVersionCreated = () => {
        refetchVersion();
        refetchRows();
    };

    const handleRowsChanged = () => {
        refetchRows();
    };

    if (isVersionLoading) {
        return (
            <MainLayout>
                <div className="flex items-center justify-center py-12">
                    <Loader2 className="h-8 w-8 animate-spin text-primary"/>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="space-y-6">
                <div className="flex items-center gap-4">
                    <Link to="/">
                        <Button variant="ghost" size="icon">
                            <ArrowLeft className="h-4 w-4"/>
                        </Button>
                    </Link>
                    <div className="flex-1">
                        <h1 className="text-2xl font-bold text-foreground">Table Details</h1>
                        <p className="text-muted-foreground">
                            {version ? `Version ${version.orderNumber}` : 'No structure defined yet'}
                        </p>
                    </div>
                    <Button onClick={() => setIsEditColumnsOpen(true)}>
                        <Settings className="mr-2 h-4 w-4"/>
                        {version ? 'Edit Structure' : 'Define Structure'}
                    </Button>
                </div>

                {!version ? (
                    <Alert>
                        <AlertCircle className="h-4 w-4"/>
                        <AlertTitle>No Table Structure</AlertTitle>
                        <AlertDescription>
                            This table doesn't have a structure defined yet. Click "Define Structure" to add
                            columns that describe the data you want to extract from PDFs.
                        </AlertDescription>
                    </Alert>
                ) : (
                    <Tabs defaultValue="data" className="space-y-6">
                        <TabsList>
                            <TabsTrigger value="data">
                                <FileSpreadsheet className="mr-2 h-4 w-4"/>
                                Data
                            </TabsTrigger>
                            <TabsTrigger value="structure">
                                <Columns3 className="mr-2 h-4 w-4"/>
                                Structure
                            </TabsTrigger>
                        </TabsList>

                        <TabsContent value="data" className="space-y-6">
                            <Card>
                                <CardHeader>
                                    <CardTitle>Extract Data from PDF</CardTitle>
                                    <CardDescription>
                                        Upload a PDF document to automatically extract data based on your table
                                        structure.
                                    </CardDescription>
                                </CardHeader>
                                <CardContent>
                                    <PdfUpload
                                        tableId={tableId!}
                                        versionNumber={version.orderNumber}
                                        onRowGenerated={handleRowsChanged}
                                    />
                                </CardContent>
                            </Card>

                            <Card>
                                <CardHeader className="flex flex-row items-center justify-between space-y-0">
                                    <div>
                                        <CardTitle>Table Data</CardTitle>
                                        <CardDescription>
                                            {rows?.length || 0} row{(rows?.length || 0) !== 1 ? 's' : ''} extracted
                                        </CardDescription>
                                    </div>
                                    <CreateRowDialog
                                        tableId={tableId!}
                                        columns={version.columns}
                                        onRowCreated={handleRowsChanged}
                                    />
                                </CardHeader>
                                <CardContent>
                                    {isRowsLoading ? (
                                        <div className="flex items-center justify-center py-8">
                                            <Loader2 className="h-6 w-6 animate-spin text-primary"/>
                                        </div>
                                    ) : (
                                        <DataTable columns={version.columns} rows={rows || []}/>
                                    )}
                                </CardContent>
                            </Card>
                        </TabsContent>

                        <TabsContent value="structure" className="space-y-4">
                            <Card>
                                <CardHeader>
                                    <CardTitle>Column Definitions</CardTitle>
                                    <CardDescription>
                                        These columns define what data will be extracted from your PDF documents.
                                    </CardDescription>
                                </CardHeader>
                                <CardContent>
                                    <div className="space-y-4">
                                        {version.columns.map((column, index) => (
                                            <div
                                                key={column.id}
                                                className="flex items-start gap-4 p-4 rounded-lg border bg-card"
                                            >
                                                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-muted text-sm font-medium">
                                                    {index + 1}
                                                </div>
                                                <div className="flex-1 min-w-0">
                                                    <div className="flex items-center gap-2 flex-wrap">
                                                        <h4 className="font-medium text-foreground">{column.name}</h4>
                                                        <Badge variant="secondary">{column.type.type}</Badge>
                                                        {column.optional && (
                                                            <Badge variant="outline">Optional</Badge>
                                                        )}
                                                    </div>
                                                    <p className="mt-1 text-sm text-muted-foreground">
                                                        {column.description}
                                                    </p>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </CardContent>
                            </Card>
                        </TabsContent>
                    </Tabs>
                )}

                <EditColumnsDialog
                    open={isEditColumnsOpen}
                    onOpenChange={setIsEditColumnsOpen}
                    tableId={tableId!}
                    existingColumns={version?.columns}
                    onVersionCreated={handleVersionCreated}
                />
            </div>
        </MainLayout>
    );
};

export default TableDetailPage;
