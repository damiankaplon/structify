import {useCallback, useEffect, useState} from 'react';
import {Link, useParams} from 'react-router-dom';
import {Tab, TabGroup, TabList, TabPanel, TabPanels} from '@headlessui/react';
import {useAuth} from '@/lib/auth';
import {
  type ColumnDefinition,
  generateRowFromPdf,
  getCurrentVersion,
  getRows,
  type Row,
  type TableVersion,
} from '@/lib/api';
import {toast} from 'sonner';
import ColumnEditor from '@/components/ColumnEditor';
import PdfUpload from '@/components/PdfUpload';
import VersionHistory from '@/components/VersionHistory';

const TableDetailPage = () => {
  const {tableId} = useParams<{ tableId: string }>();
  const {getToken} = useAuth();
  const [version, setVersion] = useState<TableVersion | null>(null);
  const [rows, setRows] = useState<Row[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    if (!tableId) return;
    try {
      setLoading(true);
      const [v, r] = await Promise.all([
        getCurrentVersion(getToken(), tableId).catch(() => null),
        getRows(getToken(), tableId).catch(() => []),
      ]);
      setVersion(v);
      setRows(r);
    } catch (e: any) {
      toast.error(e.message);
    } finally {
      setLoading(false);
    }
  }, [tableId, getToken]);

  useEffect(() => {
    load();
  }, [load]);

  const flatColumns = (cols: ColumnDefinition[]): ColumnDefinition[] =>
    cols.flatMap((c) => (c.children.length > 0 ? c.children : [c]));

  const handlePdfUpload = async (file: File) => {
    if (!tableId || !version) return;
    try {
      await generateRowFromPdf(getToken(), tableId, version.orderNumber, file);
      toast.success('Row generated from PDF');
      load();
    } catch (e: any) {
      toast.error(e.message);
    }
  };

  if (loading) {
    return (
      <div className="space-y-4">
        <div className="h-8 w-48 animate-pulse rounded-lg bg-muted"/>
        <div className="h-64 animate-pulse rounded-xl bg-muted"/>
      </div>
    );
  }

  const flat = version ? flatColumns(version.columns) : [];

  return (
    <div>
      <div className="mb-6 flex items-center gap-3">
        <Link
          to="/"
          className="flex h-8 w-8 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7"/>
          </svg>
        </Link>
        <h1 className="text-2xl font-bold text-foreground">Table Detail</h1>
      </div>

      <TabGroup>
        <TabList className="mb-6 flex gap-1 rounded-xl bg-muted p-1">
          {['Schema', 'Data', 'Upload PDF', 'Version History'].map((label) => (
            <Tab
              key={label}
              className={({selected}) =>
                `flex-1 rounded-lg px-4 py-2 text-sm font-medium transition-all focus:outline-none ${
                  selected
                    ? 'bg-card text-foreground shadow-sm'
                    : 'text-muted-foreground hover:text-foreground'
                }`
              }
            >
              {label}
            </Tab>
          ))}
        </TabList>

        <TabPanels>
          {/* Schema Tab */}
          <TabPanel>
            <ColumnEditor tableId={tableId!} version={version} onSaved={load}/>
          </TabPanel>

          {/* Data Tab */}
          <TabPanel>
            {rows.length === 0 ? (
              <div className="flex flex-col items-center rounded-2xl border-2 border-dashed border-border py-16">
                <p className="text-sm font-medium text-foreground">No rows yet</p>
                <p className="mt-1 text-xs text-muted-foreground">
                  Upload a PDF to extract data into this table
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto rounded-xl border border-border">
                <table className="w-full text-sm">
                  <thead>
                  <tr className="border-b border-border bg-muted/50">
                    {flat.map((col) => (
                      <th
                        key={col.id}
                        className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-muted-foreground"
                      >
                        {col.name}
                      </th>
                    ))}
                  </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                  {rows.map((row) => (
                    <tr key={row.id} className="transition-colors hover:bg-muted/30">
                      {flat.map((col) => {
                        const cell = row.cells.find((c) => c.columnDefinitionId === col.id);
                        return (
                          <td key={col.id} className="px-4 py-3 text-foreground">
                            {cell?.value || '—'}
                          </td>
                        );
                      })}
                    </tr>
                  ))}
                  </tbody>
                </table>
              </div>
            )}
          </TabPanel>

          {/* Upload PDF Tab */}
          <TabPanel>
            {!version ? (
              <div className="rounded-xl border border-border bg-card p-6 text-center">
                <p className="text-sm text-muted-foreground">
                  Define your table schema first before uploading documents.
                </p>
              </div>
            ) : (
              <PdfUpload onUpload={handlePdfUpload}/>
            )}
          </TabPanel>

          {/* Version History Tab */}
          <TabPanel>
            <VersionHistory
              tableId={tableId!}
              currentVersionId={version?.id ?? null}
              onRestored={load}
            />
          </TabPanel>
        </TabPanels>
      </TabGroup>
    </div>
  );
};

export default TableDetailPage;
