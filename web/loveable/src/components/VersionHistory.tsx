import {useCallback, useEffect, useState} from 'react';
import {Dialog, DialogPanel, DialogTitle} from '@headlessui/react';
import {useAuth} from '@/lib/auth';
import {type ColumnDefinition, getAllVersions, restoreVersion, type TableVersion,} from '@/lib/api';
import {toast} from 'sonner';

interface Props {
  tableId: string;
  currentVersionId: string | null;
  onRestored: () => void;
}

const VersionHistory = ({tableId, currentVersionId, onRestored}: Props) => {
  const {getToken} = useAuth();
  const [versions, setVersions] = useState<TableVersion[]>([]);
  const [loading, setLoading] = useState(true);
  const [restoring, setRestoring] = useState<number | null>(null);
  const [inspecting, setInspecting] = useState<TableVersion | null>(null);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      const all = await getAllVersions(getToken(), tableId);
      all.sort((a, b) => b.orderNumber - a.orderNumber);
      setVersions(all);
    } catch (e: any) {
      toast.error(e.message);
    } finally {
      setLoading(false);
    }
  }, [tableId, getToken]);

  useEffect(() => {
    load();
  }, [load]);

  const handleRestore = async (orderNumber: number) => {
    try {
      setRestoring(orderNumber);
      await restoreVersion(getToken(), tableId, orderNumber);
      toast.success(`Version ${orderNumber} restored`);
      await load();
      onRestored();
    } catch (e: any) {
      toast.error(e.message);
    } finally {
      setRestoring(null);
    }
  };

  const renderColumnSummary = (cols: ColumnDefinition[], depth: number = 0): JSX.Element[] =>
    cols.map((col) => (
      <div key={col.id} style={{paddingLeft: depth * 16}}>
        <div className="flex items-center gap-2 py-1.5">
          <span className="font-medium text-foreground text-sm">{col.name}</span>
          <span className="rounded bg-muted px-1.5 py-0.5 text-xs font-mono text-muted-foreground">
            {col.type.type}
            {col.type.format ? ` · ${col.type.format}` : ''}
          </span>
          {col.optional && (
            <span className="text-xs text-muted-foreground italic">optional</span>
          )}
        </div>
        {col.description && (
          <p className="text-xs text-muted-foreground" style={{paddingLeft: depth * 16 > 0 ? 0 : 0}}>
            {col.description}
          </p>
        )}
        {col.children.length > 0 && renderColumnSummary(col.children, depth + 1)}
      </div>
    ));

  if (loading) {
    return (
      <div className="space-y-3">
        {[1, 2, 3].map((i) => (
          <div key={i} className="h-20 animate-pulse rounded-xl bg-muted"/>
        ))}
      </div>
    );
  }

  if (versions.length === 0) {
    return (
      <div className="flex flex-col items-center rounded-2xl border-2 border-dashed border-border py-16">
        <p className="text-sm font-medium text-foreground">No versions yet</p>
        <p className="mt-1 text-xs text-muted-foreground">
          Define a schema to create the first version
        </p>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-4">
        <h2 className="text-lg font-semibold text-foreground">Version History</h2>
        <p className="text-xs text-muted-foreground">
          Browse previous schema versions and restore any one to make it the current version.
        </p>
      </div>

      <div className="space-y-2">
        {versions.map((v) => {
          const isCurrent = v.id === currentVersionId;
          const isRestoring = restoring === v.orderNumber;

          return (
            <div
              key={v.id}
              className={`rounded-xl border p-4 transition-all ${
                isCurrent
                  ? 'border-primary/40 bg-primary/5'
                  : 'border-border bg-card hover:border-primary/20'
              }`}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <span className="text-sm font-semibold text-foreground">
                    Version {v.orderNumber}
                  </span>
                  {isCurrent && (
                    <span className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
                      Current
                    </span>
                  )}
                  <span className="text-xs text-muted-foreground">
                    {v.columns.length} column{v.columns.length !== 1 ? 's' : ''}
                  </span>
                </div>

                <div className="flex gap-2">
                  <button
                    onClick={() => setInspecting(v)}
                    className="rounded-lg px-3 py-1.5 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                  >
                    View Schema
                  </button>
                  {!isCurrent && (
                    <button
                      onClick={() => handleRestore(v.orderNumber)}
                      disabled={restoring !== null}
                      className="rounded-lg bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground transition-all hover:bg-primary/90 disabled:opacity-50"
                    >
                      {isRestoring ? 'Restoring…' : 'Restore'}
                    </button>
                  )}
                </div>
              </div>

              {/* Inline column name previews */}
              <div className="mt-2 flex flex-wrap gap-1.5">
                {v.columns.map((col) => (
                  <span
                    key={col.id}
                    className="rounded bg-muted px-2 py-0.5 text-xs text-muted-foreground"
                  >
                    {col.name}
                  </span>
                ))}
              </div>
            </div>
          );
        })}
      </div>

      {/* Version detail dialog */}
      <Dialog open={inspecting !== null} onClose={() => setInspecting(null)} className="relative z-50">
        <div className="fixed inset-0 bg-foreground/20 backdrop-blur-sm" aria-hidden="true"/>
        <div className="fixed inset-0 flex items-center justify-center p-4">
          <DialogPanel
            className="w-full max-w-2xl rounded-2xl bg-card p-6 shadow-xl border border-border max-h-[85vh] overflow-y-auto">
            {inspecting && (
              <>
                <DialogTitle className="text-lg font-semibold text-foreground">
                  Version {inspecting.orderNumber} — Schema
                </DialogTitle>
                <p className="mt-1 text-xs text-muted-foreground mb-4">
                  {inspecting.columns.length} column{inspecting.columns.length !== 1 ? 's' : ''} defined in this
                  version.
                </p>

                <div className="space-y-1 rounded-xl border border-border bg-muted/30 p-4">
                  {renderColumnSummary(inspecting.columns)}
                </div>

                <div className="mt-6 flex justify-end gap-3">
                  {inspecting.id !== currentVersionId && (
                    <button
                      onClick={() => {
                        setInspecting(null);
                        handleRestore(inspecting.orderNumber);
                      }}
                      disabled={restoring !== null}
                      className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-sm transition-all hover:bg-primary/90 disabled:opacity-50"
                    >
                      Restore This Version
                    </button>
                  )}
                  <button
                    onClick={() => setInspecting(null)}
                    className="rounded-lg px-4 py-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
                  >
                    Close
                  </button>
                </div>
              </>
            )}
          </DialogPanel>
        </div>
      </Dialog>
    </div>
  );
};

export default VersionHistory;
