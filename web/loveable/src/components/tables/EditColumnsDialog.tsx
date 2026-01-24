import React, {useState} from 'react';
import {Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle,} from '@/components/ui/dialog';
import {Button} from '@/components/ui/button';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {Checkbox} from '@/components/ui/checkbox';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue,} from '@/components/ui/select';
import {GripVertical, Loader2, Plus, Trash2} from 'lucide-react';
import {useApi} from '@/hooks/useApi';
import {Column, CreateColumnRequest, tablesApi} from '@/lib/api';
import {useToast} from '@/hooks/use-toast';
import {Card} from '@/components/ui/card';

interface ColumnFormData {
    id: string;
    name: string;
    description: string;
    type: 'STRING' | 'NUMBER' | 'DATE' | 'BOOLEAN';
    optional: boolean;
}

interface EditColumnsDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    tableId: string;
    existingColumns?: Column[];
    onVersionCreated: () => void;
}

const createEmptyColumn = (): ColumnFormData => ({
    id: crypto.randomUUID(),
    name: '',
    description: '',
    type: 'STRING',
    optional: false,
});

const EditColumnsDialog: React.FC<EditColumnsDialogProps> = ({
                                                                 open,
                                                                 onOpenChange,
                                                                 tableId,
                                                                 existingColumns,
                                                                 onVersionCreated,
                                                             }) => {
    const [columns, setColumns] = useState<ColumnFormData[]>(() => {
        if (existingColumns && existingColumns.length > 0) {
            return existingColumns.map((col) => ({
                id: col.id,
                name: col.name,
                description: col.description,
                type: col.type.type,
                optional: col.optional,
            }));
        }
        return [createEmptyColumn()];
    });
    const [isLoading, setIsLoading] = useState(false);
    const api = useApi();
    const {toast} = useToast();

    const addColumn = () => {
        setColumns([...columns, createEmptyColumn()]);
    };

    const removeColumn = (id: string) => {
        if (columns.length <= 1) return;
        setColumns(columns.filter((col) => col.id !== id));
    };

    const updateColumn = (id: string, field: keyof ColumnFormData, value: any) => {
        setColumns(
            columns.map((col) => (col.id === id ? {...col, [field]: value} : col))
        );
    };

    const handleSubmit = async () => {
        const invalidColumns = columns.filter((col) => !col.name.trim() || !col.description.trim());
        if (invalidColumns.length > 0) {
            toast({
                title: 'Validation Error',
                description: 'All columns must have a name and description',
                variant: 'destructive',
            });
            return;
        }

        setIsLoading(true);
        try {
            const columnRequests: CreateColumnRequest[] = columns.map((col) => {
                const type = col.type === 'DATE' ? 'STRING' : col.type;
                const stringFormat = col.type === 'DATE' ? 'DATE' : null;
                return ({
                    name: col.name,
                    description: col.description,
                    type: type,
                    stringFormat: stringFormat,
                    optional: col.optional,
                })
            });

            await tablesApi.createVersion(api, tableId, columnRequests);
            toast({
                title: 'Success',
                description: 'Table structure updated successfully',
            });
            onOpenChange(false);
            onVersionCreated();
        } catch (error) {
            toast({
                title: 'Error',
                description: 'Failed to update table structure',
                variant: 'destructive',
            });
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
                <DialogHeader>
                    <DialogTitle>Define Table Structure</DialogTitle>
                    <DialogDescription>
                        Define the columns for this table. Each column should have a clear name and
                        detailed description to help the AI extract the correct information from PDFs.
                    </DialogDescription>
                </DialogHeader>
                <div className="space-y-4 py-4">
                    {columns.map((column, index) => (
                        <Card key={column.id} className="p-4">
                            <div className="flex items-start gap-4">
                                <div className="flex items-center pt-2 text-muted-foreground">
                                    <GripVertical className="h-5 w-5"/>
                                    <span className="ml-1 text-sm font-medium">{index + 1}</span>
                                </div>
                                <div className="flex-1 grid gap-4">
                                    <div className="grid grid-cols-2 gap-4">
                                        <div className="space-y-2">
                                            <Label>Column Name</Label>
                                            <Input
                                                value={column.name}
                                                onChange={(e) => updateColumn(column.id, 'name', e.target.value)}
                                                placeholder="e.g., First Name"
                                            />
                                        </div>
                                        <div className="space-y-2">
                                            <Label>Type</Label>
                                            <Select
                                                value={column.type}
                                                onValueChange={(value) => updateColumn(column.id, 'type', value)}
                                            >
                                                <SelectTrigger>
                                                    <SelectValue/>
                                                </SelectTrigger>
                                                <SelectContent>
                                                    <SelectItem value="STRING">Text</SelectItem>
                                                    <SelectItem value="NUMBER">Number</SelectItem>
                                                    <SelectItem value="DATE">Date</SelectItem>
                                                    <SelectItem value="BOOLEAN">Yes/No</SelectItem>
                                                </SelectContent>
                                            </Select>
                                        </div>
                                    </div>
                                    <div className="space-y-2">
                                        <Label>Description</Label>
                                        <Textarea
                                            value={column.description}
                                            onChange={(e) => updateColumn(column.id, 'description', e.target.value)}
                                            placeholder="Describe what this column should contain. Be specific to help the AI find this information in documents."
                                            rows={2}
                                        />
                                    </div>
                                    <div className="flex items-center space-x-2">
                                        <Checkbox
                                            id={`optional-${column.id}`}
                                            checked={column.optional}
                                            onCheckedChange={(checked) =>
                                                updateColumn(column.id, 'optional', checked)
                                            }
                                        />
                                        <Label htmlFor={`optional-${column.id}`} className="text-sm font-normal">
                                            Optional (value may not exist in some documents)
                                        </Label>
                                    </div>
                                </div>
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="icon"
                                    onClick={() => removeColumn(column.id)}
                                    disabled={columns.length <= 1}
                                    className="text-muted-foreground hover:text-destructive"
                                >
                                    <Trash2 className="h-4 w-4"/>
                                </Button>
                            </div>
                        </Card>
                    ))}
                    <Button type="button" variant="outline" onClick={addColumn} className="w-full">
                        <Plus className="mr-2 h-4 w-4"/>
                        Add Column
                    </Button>
                </div>
                <DialogFooter>
                    <Button variant="outline" onClick={() => onOpenChange(false)}>
                        Cancel
                    </Button>
                    <Button onClick={handleSubmit} disabled={isLoading}>
                        {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin"/>}
                        Save Structure
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default EditColumnsDialog;
