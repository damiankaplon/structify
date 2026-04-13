import {useState} from 'react';
import {
  Dialog,
  DialogPanel,
  DialogTitle,
  Listbox,
  ListboxButton,
  ListboxOption,
  ListboxOptions
} from '@headlessui/react';
import {useAuth} from '@/lib/auth';
import {type ColumnDefinition, type ColumnInput, createVersion, type TableVersion} from '@/lib/api';
import {toast} from 'sonner';

const TYPES = ['STRING', 'NUMBER', 'OBJECT'] as const;
const STRING_FORMATS = [null, 'DATE'] as const;

export interface ColumnFormData {
  name: string;
  description: string;
  type: 'STRING' | 'NUMBER' | 'OBJECT';
  stringFormat: string | null;
  children: ColumnFormData[];
}

const emptyColumn = (): ColumnFormData => ({
  name: '',
  description: '',
  type: 'STRING',
  stringFormat: null,
  children: [],
});

interface Props {
  tableId: string;
  version: TableVersion | null;
  onSaved: () => void;
}

const ColumnEditor = ({tableId, version, onSaved}: Props) => {
  const {getToken} = useAuth();

  const mapColumn = (c: ColumnDefinition): ColumnFormData => ({
    name: c.name,
    description: c.description,
    type: c.type.type,
    stringFormat: c.type.format,
    children: c.children.map(mapColumn),
  });

  const initialColumns: ColumnFormData[] = version ? version.columns.map(mapColumn) : [];

  const [columns, setColumns] = useState<ColumnFormData[]>(initialColumns);
  const [saving, setSaving] = useState(false);

  // Field editor dialog state — path is the parent path ([] = root), index is null for add, number for edit
  const [fieldDialog, setFieldDialog] = useState<{
    parentPath: number[];
    index: number | null;
    data: ColumnFormData;
  } | null>(null);

  // Object drill-down dialog — path to the OBJECT column being viewed
  const [objectViewPath, setObjectViewPath] = useState<number[] | null>(null);

  // --- Helpers to read/write columns at a nested path ---
  const getColumnsAtPath = (path: number[]): ColumnFormData[] => {
    let current = columns;
    for (const idx of path) {
      current = current[idx].children;
    }
    return current;
  };

  const getColumnAtPath = (path: number[]): ColumnFormData => {
    let current: ColumnFormData = columns[path[0]];
    for (let i = 1; i < path.length; i++) {
      current = current.children[path[i]];
    }
    return current;
  };

  const updateColumnsAtPath = (path: number[], updater: (cols: ColumnFormData[]) => ColumnFormData[]) => {
    if (path.length === 0) {
      setColumns(updater(columns));
      return;
    }
    const newColumns: ColumnFormData[] = JSON.parse(JSON.stringify(columns));
    let current = newColumns;
    for (let i = 0; i < path.length - 1; i++) {
      current = current[path[i]].children;
    }
    current[path[path.length - 1]].children = updater(current[path[path.length - 1]].children);
    setColumns(newColumns);
  };

  // --- Validation: no OBJECT with 0 children ---
  const hasEmptyObjects = (cols: ColumnFormData[]): boolean => {
    for (const col of cols) {
      if (col.type === 'OBJECT') {
        if (col.children.length === 0) return true;
        if (hasEmptyObjects(col.children)) return true;
      }
    }
    return false;
  };

  const canSave = columns.length > 0 && !hasEmptyObjects(columns);

  // --- Field dialog actions ---
  const openAddField = (parentPath: number[]) => {
    setFieldDialog({parentPath, index: null, data: emptyColumn()});
  };

  const openEditField = (parentPath: number[], index: number) => {
    const cols = getColumnsAtPath(parentPath);
    setFieldDialog({parentPath, index, data: {...cols[index], children: [...cols[index].children]}});
  };

  const saveField = () => {
    if (!fieldDialog || !fieldDialog.data.name.trim()) return;
    const {parentPath, index, data} = fieldDialog;
    // When saving, if type changed away from OBJECT, clear children
    const cleaned = {...data, children: data.type === 'OBJECT' ? data.children : []};
    updateColumnsAtPath(parentPath, (cols) => {
      const updated = [...cols];
      if (index !== null && index >= 0) {
        updated[index] = cleaned;
      } else {
        updated.push(cleaned);
      }
      return updated;
    });
    setFieldDialog(null);
  };

  const removeField = (parentPath: number[], index: number) => {
    updateColumnsAtPath(parentPath, (cols) => cols.filter((_, i) => i !== index));
  };

  // --- Object drill-down ---
  const openObjectView = (path: number[]) => {
    setObjectViewPath(path);
  };

  const getBreadcrumb = (): { name: string; path: number[] }[] => {
    if (!objectViewPath) return [];
    const crumbs: { name: string; path: number[] }[] = [];
    for (let i = 0; i < objectViewPath.length; i++) {
      const subPath = objectViewPath.slice(0, i + 1);
      crumbs.push({name: getColumnAtPath(subPath).name || 'Unnamed', path: subPath});
    }
    return crumbs;
  };

  // --- API save ---
  const toInput = (c: ColumnFormData): ColumnInput => ({
    name: c.name,
    description: c.description,
    type: c.type,
    ...(c.type === 'STRING' && c.stringFormat ? {stringFormat: c.stringFormat} : {}),
    ...(c.type === 'OBJECT' && c.children.length > 0 ? {children: c.children.map(toInput)} : {}),
  });

  const handleSaveSchema = async () => {
    if (!canSave) return;
    try {
      setSaving(true);
      await createVersion(getToken(), tableId, columns.map(toInput));
      toast.success('Schema saved — new version created');
      onSaved();
    } catch (e: any) {
      toast.error(e.message);
    } finally {
      setSaving(false);
    }
  };

  // --- Column tile renderer (reused for root & object dialog) ---
  const renderColumnTile = (col: ColumnFormData, idx: number, parentPath: number[]) => {
    const isObject = col.type === 'OBJECT';
    const childCount = col.children.length;
    const hasEmptyObj = isObject && childCount === 0;

    return (
      <div
        key={idx}
        className={`flex items-start justify-between rounded-xl border bg-card p-4 transition-all ${
          hasEmptyObj ? 'border-destructive/40' : 'border-border hover:border-primary/20'
        } ${isObject ? 'cursor-pointer' : ''}`}
        onClick={isObject ? () => openObjectView([...parentPath, idx]) : undefined}
      >
        <div className="flex-1">
          <div className="flex items-center gap-2">
            <span className="font-medium text-foreground">{col.name}</span>
            <span className="rounded bg-muted px-2 py-0.5 text-xs font-mono text-muted-foreground">
              {col.type}
              {col.stringFormat ? ` · ${col.stringFormat}` : ''}
            </span>
          </div>
          <p className="mt-1 text-xs text-muted-foreground">{col.description}</p>
          {isObject && (
            <div className="mt-2">
              {childCount === 0 ? (
                <span className="text-xs text-destructive">⚠ No fields defined — click to add</span>
              ) : (
                <span className="text-xs text-muted-foreground">
                  {childCount} field{childCount !== 1 ? 's' : ''} — click to edit
                </span>
              )}
            </div>
          )}
        </div>
        <div className="flex gap-1 ml-4" onClick={(e) => e.stopPropagation()}>
          <button
            onClick={() => openEditField(parentPath, idx)}
            className="rounded-lg p-1.5 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          >
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                    d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
            </svg>
          </button>
          <button
            onClick={() => removeField(parentPath, idx)}
            className="rounded-lg p-1.5 text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive"
          >
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                    d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
            </svg>
          </button>
        </div>
      </div>
    );
  };

  // --- Render ---
  const objectCols = objectViewPath ? getColumnsAtPath(objectViewPath) : [];
  const breadcrumb = getBreadcrumb();

  return (
    <div>
      {/* Header */}
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold text-foreground">Columns</h2>
          <p className="text-xs text-muted-foreground">
            Describe each column so the AI knows what to extract from your documents.
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => openAddField([])}
            className="rounded-lg bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground transition-all hover:bg-primary/90"
          >
            + Column
          </button>
          <button
            onClick={handleSaveSchema}
            disabled={saving || !canSave}
            title={!canSave && columns.length > 0 ? 'All OBJECT columns must have at least one field' : undefined}
            className="rounded-lg bg-accent px-3 py-1.5 text-sm font-medium text-accent-foreground transition-all hover:bg-accent/90 disabled:opacity-50"
          >
            {saving ? 'Saving…' : 'Save Schema'}
          </button>
        </div>
      </div>

      {/* Column list */}
      {columns.length === 0 ? (
        <div className="flex flex-col items-center rounded-2xl border-2 border-dashed border-border py-12">
          <p className="text-sm font-medium text-foreground">No columns defined</p>
          <p className="mt-1 text-xs text-muted-foreground">Add columns to define what data the AI should extract</p>
        </div>
      ) : (
        <div className="space-y-2">
          {columns.map((col, idx) => renderColumnTile(col, idx, []))}
        </div>
      )}

      {/* Field Add/Edit Dialog (simple — no children) */}
      <Dialog open={fieldDialog !== null} onClose={() => setFieldDialog(null)} className="relative z-50">
        <div className="fixed inset-0 bg-foreground/20 backdrop-blur-sm" aria-hidden="true"/>
        <div className="fixed inset-0 flex items-center justify-center p-4">
          <DialogPanel className="w-full max-w-lg rounded-2xl bg-card p-6 shadow-xl border border-border">
            <DialogTitle className="text-lg font-semibold text-foreground">
              {fieldDialog?.index !== null && fieldDialog?.index !== undefined && fieldDialog.index >= 0 ? 'Edit Column' : 'Add Column'}
            </DialogTitle>

            {fieldDialog && (
              <div className="mt-4 space-y-4">
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-foreground">Name</label>
                  <input
                    value={fieldDialog.data.name}
                    onChange={(e) => setFieldDialog({
                      ...fieldDialog,
                      data: {...fieldDialog.data, name: e.target.value}
                    })}
                    placeholder="e.g. Amount"
                    className="w-full rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                  />
                </div>

                <div>
                  <label className="mb-1.5 block text-sm font-medium text-foreground">
                    Description
                    <span
                      className="ml-1 text-xs font-normal text-muted-foreground">(helps AI extract this field)</span>
                  </label>
                  <textarea
                    value={fieldDialog.data.description}
                    onChange={(e) => setFieldDialog({
                      ...fieldDialog,
                      data: {...fieldDialog.data, description: e.target.value}
                    })}
                    rows={3}
                    placeholder="Describe what this column should contain…"
                    className="w-full rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring resize-none"
                  />
                </div>

                <div>
                  <label className="mb-1.5 block text-sm font-medium text-foreground">Type</label>
                  <Listbox
                    value={fieldDialog.data.type}
                    onChange={(v) =>
                      setFieldDialog({
                        ...fieldDialog,
                        data: {...fieldDialog.data, type: v, children: v === 'OBJECT' ? fieldDialog.data.children : []},
                      })
                    }
                  >
                    <div className="relative">
                      <ListboxButton
                        className="w-full rounded-lg border border-input bg-background px-3 py-2 text-left text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-ring">
                        {fieldDialog.data.type}
                      </ListboxButton>
                      <ListboxOptions
                        className="absolute z-10 mt-1 w-full rounded-lg border border-border bg-card py-1 shadow-lg focus:outline-none">
                        {TYPES.map((t) => (
                          <ListboxOption
                            key={t}
                            value={t}
                            className={({active}) =>
                              `cursor-pointer px-3 py-2 text-sm ${active ? 'bg-primary/10 text-primary' : 'text-foreground'}`
                            }
                          >
                            {t}
                          </ListboxOption>
                        ))}
                      </ListboxOptions>
                    </div>
                  </Listbox>
                </div>

                {fieldDialog.data.type === 'STRING' && (
                  <div>
                    <label className="mb-1.5 block text-sm font-medium text-foreground">Format</label>
                    <Listbox
                      value={fieldDialog.data.stringFormat}
                      onChange={(v) => setFieldDialog({...fieldDialog, data: {...fieldDialog.data, stringFormat: v}})}
                    >
                      <div className="relative">
                        <ListboxButton
                          className="w-full rounded-lg border border-input bg-background px-3 py-2 text-left text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-ring">
                          {fieldDialog.data.stringFormat || 'None'}
                        </ListboxButton>
                        <ListboxOptions
                          className="absolute z-10 mt-1 w-full rounded-lg border border-border bg-card py-1 shadow-lg focus:outline-none">
                          {STRING_FORMATS.map((f) => (
                            <ListboxOption
                              key={f ?? 'none'}
                              value={f}
                              className={({active}) =>
                                `cursor-pointer px-3 py-2 text-sm ${active ? 'bg-primary/10 text-primary' : 'text-foreground'}`
                              }
                            >
                              {f || 'None'}
                            </ListboxOption>
                          ))}
                        </ListboxOptions>
                      </div>
                    </Listbox>
                  </div>
                )}

                {fieldDialog.data.type === 'OBJECT' && (
                  <p className="rounded-lg bg-muted/50 px-3 py-2 text-xs text-muted-foreground">
                    After saving, click on this column tile to define its child fields.
                  </p>
                )}
              </div>
            )}

            <div className="mt-6 flex justify-end gap-3">
              <button
                onClick={() => setFieldDialog(null)}
                className="rounded-lg px-4 py-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={saveField}
                disabled={!fieldDialog?.data.name.trim()}
                className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-sm transition-all hover:bg-primary/90 disabled:opacity-50"
              >
                {fieldDialog?.index !== null && fieldDialog?.index !== undefined && fieldDialog.index >= 0 ? 'Update' : 'Add'}
              </button>
            </div>
          </DialogPanel>
        </div>
      </Dialog>

      {/* Object Fields Drill-Down Dialog */}
      <Dialog open={objectViewPath !== null} onClose={() => setObjectViewPath(null)} className="relative z-50">
        <div className="fixed inset-0 bg-foreground/20 backdrop-blur-sm" aria-hidden="true"/>
        <div className="fixed inset-0 flex items-center justify-center p-4">
          <DialogPanel
            className="w-full max-w-2xl rounded-2xl bg-card p-6 shadow-xl border border-border max-h-[85vh] overflow-y-auto">
            {objectViewPath && (
              <>
                {/* Breadcrumb */}
                <div className="mb-4 flex items-center gap-1.5 text-sm">
                  <button
                    onClick={() => setObjectViewPath(null)}
                    className="text-muted-foreground hover:text-foreground transition-colors"
                  >
                    Root
                  </button>
                  {breadcrumb.map((crumb, i) => (
                    <span key={i} className="flex items-center gap-1.5">
                      <span className="text-muted-foreground">/</span>
                      {i < breadcrumb.length - 1 ? (
                        <button
                          onClick={() => setObjectViewPath(crumb.path)}
                          className="text-muted-foreground hover:text-foreground transition-colors"
                        >
                          {crumb.name}
                        </button>
                      ) : (
                        <span className="font-medium text-foreground">{crumb.name}</span>
                      )}
                    </span>
                  ))}
                </div>

                <DialogTitle className="text-lg font-semibold text-foreground">
                  Fields of "{getColumnAtPath(objectViewPath).name || 'Unnamed'}"
                </DialogTitle>
                <p className="mt-1 text-xs text-muted-foreground mb-4">
                  Define the child fields that make up this object column.
                </p>

                <div className="mb-4 flex justify-end">
                  <button
                    onClick={() => openAddField(objectViewPath)}
                    className="rounded-lg bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground transition-all hover:bg-primary/90"
                  >
                    + Field
                  </button>
                </div>

                {objectCols.length === 0 ? (
                  <div
                    className="flex flex-col items-center rounded-2xl border-2 border-dashed border-destructive/30 py-10">
                    <p className="text-sm font-medium text-foreground">No fields defined</p>
                    <p className="mt-1 text-xs text-muted-foreground">This object needs at least one field before saving
                      the schema</p>
                  </div>
                ) : (
                  <div className="space-y-2">
                    {objectCols.map((col, idx) => renderColumnTile(col, idx, objectViewPath))}
                  </div>
                )}

                <div className="mt-6 flex justify-end">
                  <button
                    onClick={() => setObjectViewPath(null)}
                    className="rounded-lg px-4 py-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
                  >
                    Done
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

export default ColumnEditor;
