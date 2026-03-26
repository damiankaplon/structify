import {Listbox, ListboxButton, ListboxOption, ListboxOptions} from '@headlessui/react';

const TYPES = ['STRING', 'NUMBER', 'OBJECT'] as const;
const STRING_FORMATS = [null, 'DATE'] as const;

export interface ColumnFormData {
    name: string;
    description: string;
    type: 'STRING' | 'NUMBER' | 'OBJECT';
    stringFormat: string | null;
    children: ColumnFormData[];
}

export const emptyColumn = (): ColumnFormData => ({
    name: '',
    description: '',
    type: 'STRING',
    stringFormat: null,
    children: [],
});

interface Props {
    field: ColumnFormData;
    onChange: (updated: ColumnFormData) => void;
    onRemove: () => void;
    depth?: number;
    label?: string;
}

const ColumnFieldEditor = ({field, onChange, onRemove, depth = 0, label}: Props) => {
    const update = (partial: Partial<ColumnFormData>) => {
        const updated = {...field, ...partial};
        if (partial.type && partial.type !== 'OBJECT') {
            updated.children = [];
        }
        onChange(updated);
    };

    const addChild = () => {
        onChange({...field, children: [...field.children, emptyColumn()]});
    };

    const updateChild = (idx: number, child: ColumnFormData) => {
        const children = [...field.children];
        children[idx] = child;
        onChange({...field, children});
    };

    const removeChild = (idx: number) => {
        onChange({...field, children: field.children.filter((_, i) => i !== idx)});
    };

    return (
        <div className="rounded-lg border border-border bg-card p-3">
            <div className="mb-2 flex items-center justify-between">
                <span className="text-xs font-medium text-muted-foreground">{label ?? 'Field'}</span>
                <button onClick={onRemove} className="text-xs text-destructive hover:underline">
                    Remove
                </button>
            </div>
            <div className="space-y-2">
                <input
                    value={field.name}
                    onChange={(e) => update({name: e.target.value})}
                    placeholder="Field name"
                    className="w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                />
                <textarea
                    value={field.description}
                    onChange={(e) => update({description: e.target.value})}
                    rows={2}
                    placeholder="Describe this field for AI extraction…"
                    className="w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring resize-none"
                />
                <Listbox value={field.type} onChange={(v) => update({type: v})}>
                    <div className="relative">
                        <ListboxButton
                            className="w-full rounded-lg border border-input bg-background px-3 py-1.5 text-left text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-ring">
                            {field.type}
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

                {field.type === 'STRING' && (
                    <Listbox value={field.stringFormat} onChange={(v) => update({stringFormat: v})}>
                        <div className="relative">
                            <ListboxButton
                                className="w-full rounded-lg border border-input bg-background px-3 py-1.5 text-left text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-ring">
                                Format: {field.stringFormat || 'None'}
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
                )}

                {field.type === 'OBJECT' && (
                    <div className="mt-2 rounded-xl border border-border bg-muted/30 p-3"
                         style={{marginLeft: depth > 0 ? 0 : undefined}}>
                        <div className="mb-2 flex items-center justify-between">
                            <span className="text-xs font-medium text-foreground">Child Fields</span>
                            <button
                                onClick={addChild}
                                className="rounded-lg bg-primary/10 px-2.5 py-1 text-xs font-medium text-primary transition-colors hover:bg-primary/20"
                            >
                                + Add Child
                            </button>
                        </div>
                        {field.children.length === 0 ? (
                            <p className="text-xs text-muted-foreground">No child fields. Add fields that make up this
                                object.</p>
                        ) : (
                            <div className="space-y-2">
                                {field.children.map((ch, ci) => (
                                    <ColumnFieldEditor
                                        key={ci}
                                        field={ch}
                                        onChange={(updated) => updateChild(ci, updated)}
                                        onRemove={() => removeChild(ci)}
                                        depth={depth + 1}
                                        label={`Field ${ci + 1}`}
                                    />
                                ))}
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default ColumnFieldEditor;
