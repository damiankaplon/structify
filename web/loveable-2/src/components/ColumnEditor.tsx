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
import {createVersion, type TableVersion, type ColumnInput, type ColumnDefinition} from '@/lib/api';
import {toast} from 'sonner';
import ColumnFieldEditor, {emptyColumn, type ColumnFormData} from '@/components/ColumnFieldEditor';

const TYPES = ['STRING', 'NUMBER', 'OBJECT'] as const;
const STRING_FORMATS = [null, 'DATE'] as const;

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

    const initialColumns: ColumnFormData[] = version
        ? version.columns.map(mapColumn)
        : [];

    const [columns, setColumns] = useState<ColumnFormData[]>(initialColumns);
    const [saving, setSaving] = useState(false);
    const [editIdx, setEditIdx] = useState<number | null>(null);
    const [editCol, setEditCol] = useState<ColumnFormData>(emptyColumn());
    const [editingChild, setEditingChild] = useState<number | null>(null);

    const openAdd = () => {
        setEditIdx(-1);
        setEditCol(emptyColumn());
    };

    const openEdit = (idx: number) => {
        setEditIdx(idx);
        setEditCol({...columns[idx], children: [...columns[idx].children]});
    };

    const closeEditor = () => setEditIdx(null);

    const saveColumn = () => {
        if (!editCol.name.trim()) return;
        const updated = [...columns];
        if (editIdx !== null && editIdx >= 0) {
            updated[editIdx] = editCol;
        } else {
            updated.push(editCol);
        }
        setColumns(updated);
        setEditIdx(null);
    };

    const removeColumn = (idx: number) => setColumns(columns.filter((_, i) => i !== idx));

    const addChild = () => {
        setEditCol({
            ...editCol,
            children: [...editCol.children, emptyColumn()],
        });
    };

    const updateChild = (idx: number, updated: ColumnFormData | Partial<ColumnFormData>) => {
        const children = [...editCol.children];
        children[idx] = {...children[idx], ...updated};
        setEditCol({...editCol, children});
    };

    const removeChild = (idx: number) => {
        setEditCol({
            ...editCol,
            children: editCol.children.filter((_, i) => i !== idx),
        });
    };

    const toInput = (c: ColumnFormData): ColumnInput => ({
        name: c.name,
        description: c.description,
        type: c.type,
        ...(c.type === 'STRING' && c.stringFormat ? {stringFormat: c.stringFormat} : {}),
        ...(c.type === 'OBJECT' && c.children.length > 0
            ? {children: c.children.map(toInput)}
            : {}),
    });

    const handleSaveSchema = async () => {
        if (columns.length === 0) {
            toast.error('Add at least one column');
            return;
        }
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

    const isEditorOpen = editIdx !== null;

    return (
        <div>
            <div className="mb-4 flex items-center justify-between">
                <div>
                    <h2 className="text-lg font-semibold text-foreground">Columns</h2>
                    <p className="text-xs text-muted-foreground">
                        Describe each column so the AI knows what to extract from your documents.
                    </p>
                </div>
                <div className="flex gap-2">
                    <button
                        onClick={() => openAdd()}
                        className="rounded-lg bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground transition-all hover:bg-primary/90"
                    >
                        + Column
                    </button>
                    <button
                        onClick={handleSaveSchema}
                        disabled={saving || columns.length === 0}
                        className="rounded-lg bg-accent px-3 py-1.5 text-sm font-medium text-accent-foreground transition-all hover:bg-accent/90 disabled:opacity-50"
                    >
                        {saving ? 'Saving…' : 'Save Schema'}
                    </button>
                </div>
            </div>

            {columns.length === 0 ? (
                <div className="flex flex-col items-center rounded-2xl border-2 border-dashed border-border py-12">
                    <p className="text-sm font-medium text-foreground">No columns defined</p>
                    <p className="mt-1 text-xs text-muted-foreground">
                        Add columns to define what data the AI should extract
                    </p>
                </div>
            ) : (
                <div className="space-y-2">
                    {columns.map((col, idx) => (
                        <div
                            key={idx}
                            className="flex items-start justify-between rounded-xl border border-border bg-card p-4 transition-all hover:border-primary/20"
                        >
                            <div className="flex-1">
                                <div className="flex items-center gap-2">
                                    <span className="font-medium text-foreground">{col.name}</span>
                                    <span
                                        className="rounded bg-muted px-2 py-0.5 text-xs font-mono text-muted-foreground">
                    {col.type}
                                        {col.stringFormat ? ` · ${col.stringFormat}` : ''}
                  </span>
                                </div>
                                <p className="mt-1 text-xs text-muted-foreground">{col.description}</p>
                                {col.children.length > 0 && (
                                    <div className="mt-2 ml-4 space-y-1">
                                        {col.children.map((ch, ci) => (
                                            <div key={ci} className="flex items-center gap-2 text-xs">
                                                <span className="text-foreground">{ch.name}</span>
                                                <span
                                                    className="rounded bg-muted px-1.5 py-0.5 font-mono text-muted-foreground">
                          {ch.type}
                        </span>
                                                <span className="text-muted-foreground">— {ch.description}</span>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                            <div className="flex gap-1 ml-4">
                                <button
                                    onClick={() => openEdit(idx)}
                                    className="rounded-lg p-1.5 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                                >
                                    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                                              d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                                    </svg>
                                </button>
                                <button
                                    onClick={() => removeColumn(idx)}
                                    className="rounded-lg p-1.5 text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive"
                                >
                                    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                                              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                                    </svg>
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Column Editor Dialog */}
            <Dialog open={isEditorOpen} onClose={closeEditor} className="relative z-50">
                <div className="fixed inset-0 bg-foreground/20 backdrop-blur-sm" aria-hidden="true"/>
                <div className="fixed inset-0 flex items-center justify-center p-4">
                    <DialogPanel
                        className="w-full max-w-lg rounded-2xl bg-card p-6 shadow-xl border border-border max-h-[85vh] overflow-y-auto">
                        <DialogTitle className="text-lg font-semibold text-foreground">
                            {editIdx !== null && editIdx >= 0 ? 'Edit Column' : 'Add Column'}
                        </DialogTitle>

                        <div className="mt-4 space-y-4">
                            <div>
                                <label className="mb-1.5 block text-sm font-medium text-foreground">Name</label>
                                <input
                                    value={editCol.name}
                                    onChange={(e) => setEditCol({...editCol, name: e.target.value})}
                                    placeholder="e.g. Amount"
                                    className="w-full rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                                />
                            </div>

                            <div>
                                <label className="mb-1.5 block text-sm font-medium text-foreground">
                                    Description
                                    <span className="ml-1 text-xs font-normal text-muted-foreground">(helps AI extract this field)</span>
                                </label>
                                <textarea
                                    value={editCol.description}
                                    onChange={(e) => setEditCol({...editCol, description: e.target.value})}
                                    rows={3}
                                    placeholder="Describe what this column should contain so the AI knows what to look for…"
                                    className="w-full rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring resize-none"
                                />
                            </div>

                            <div>
                                <label className="mb-1.5 block text-sm font-medium text-foreground">Type</label>
                                <Listbox value={editCol.type} onChange={(v) => setEditCol({
                                    ...editCol,
                                    type: v,
                                    children: v === 'OBJECT' ? editCol.children : []
                                })}>
                                    <div className="relative">
                                        <ListboxButton
                                            className="w-full rounded-lg border border-input bg-background px-3 py-2 text-left text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-ring">
                                            {editCol.type}
                                        </ListboxButton>
                                        <ListboxOptions
                                            className="absolute z-10 mt-1 w-full rounded-lg border border-border bg-card py-1 shadow-lg focus:outline-none">
                                            {TYPES.map((t) => (
                                                <ListboxOption
                                                    key={t}
                                                    value={t}
                                                    className={({active}) =>
                                                        `cursor-pointer px-3 py-2 text-sm ${
                                                            active ? 'bg-primary/10 text-primary' : 'text-foreground'
                                                        }`
                                                    }
                                                >
                                                    {t}
                                                </ListboxOption>
                                            ))}
                                        </ListboxOptions>
                                    </div>
                                </Listbox>
                            </div>

                            {editCol.type === 'STRING' && (
                                <div>
                                    <label className="mb-1.5 block text-sm font-medium text-foreground">Format</label>
                                    <Listbox value={editCol.stringFormat}
                                             onChange={(v) => setEditCol({...editCol, stringFormat: v})}>
                                        <div className="relative">
                                            <ListboxButton
                                                className="w-full rounded-lg border border-input bg-background px-3 py-2 text-left text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-ring">
                                                {editCol.stringFormat || 'None'}
                                            </ListboxButton>
                                            <ListboxOptions
                                                className="absolute z-10 mt-1 w-full rounded-lg border border-border bg-card py-1 shadow-lg focus:outline-none">
                                                {STRING_FORMATS.map((f) => (
                                                    <ListboxOption
                                                        key={f ?? 'none'}
                                                        value={f}
                                                        className={({active}) =>
                                                            `cursor-pointer px-3 py-2 text-sm ${
                                                                active ? 'bg-primary/10 text-primary' : 'text-foreground'
                                                            }`
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

                            {editCol.type === 'OBJECT' && (
                                <div className="rounded-xl border border-border bg-muted/30 p-4">
                                    <div className="mb-3 flex items-center justify-between">
                                        <span className="text-sm font-medium text-foreground">Child Fields</span>
                                        <button
                                            onClick={addChild}
                                            className="rounded-lg bg-primary/10 px-2.5 py-1 text-xs font-medium text-primary transition-colors hover:bg-primary/20"
                                        >
                                            + Add Child
                                        </button>
                                    </div>
                                    {editCol.children.length === 0 ? (
                                        <p className="text-xs text-muted-foreground">No child fields. Add fields that
                                            make up this object.</p>
                                    ) : (
                                        <div className="space-y-3">
                                            {editCol.children.map((ch, ci) => (
                                                <ColumnFieldEditor
                                                    key={ci}
                                                    field={ch}
                                                    onChange={(updated) => updateChild(ci, updated)}
                                                    onRemove={() => removeChild(ci)}
                                                    depth={1}
                                                    label={`Field ${ci + 1}`}
                                                />
                                            ))}
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>

                        <div className="mt-6 flex justify-end gap-3">
                            <button
                                onClick={closeEditor}
                                className="rounded-lg px-4 py-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={saveColumn}
                                disabled={!editCol.name.trim()}
                                className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-sm transition-all hover:bg-primary/90 disabled:opacity-50"
                            >
                                {editIdx !== null && editIdx >= 0 ? 'Update' : 'Add'}
                            </button>
                        </div>
                    </DialogPanel>
                </div>
            </Dialog>
        </div>
    );
};

export default ColumnEditor;
