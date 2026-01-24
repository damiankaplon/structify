import React, {useState} from 'react';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Button} from '@/components/ui/button';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Plus, Loader2} from 'lucide-react';
import {useApi} from '@/hooks/useApi';
import {tablesApi, Column} from '@/lib/api';
import {useToast} from '@/hooks/use-toast';

interface CreateRowDialogProps {
    tableId: string;
    columns: Column[];
    onRowCreated: () => void;
}

const CreateRowDialog: React.FC<CreateRowDialogProps> = ({tableId, columns, onRowCreated}) => {
    const [open, setOpen] = useState(false);
    const [values, setValues] = useState<Record<string, string>>({});
    const [isLoading, setIsLoading] = useState(false);
    const api = useApi();
    const {toast} = useToast();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        const requiredColumns = columns.filter((col) => !col.optional);
        const missingRequired = requiredColumns.filter((col) => !values[col.id]?.trim());

        if (missingRequired.length > 0) {
            toast({
                title: 'Validation Error',
                description: `Please fill in required fields: ${missingRequired.map((c) => c.name).join(', ')}`,
                variant: 'destructive',
            });
            return;
        }

        setIsLoading(true);
        try {
            const cells = Object.entries(values)
                .filter(([_, value]) => value.trim())
                .map(([columnId, value]) => ({columnId, value}));

            await tablesApi.createRow(api, tableId, {cells});
            toast({
                title: 'Success',
                description: 'Row created successfully',
            });
            setOpen(false);
            setValues({});
            onRowCreated();
        } catch (error) {
            toast({
                title: 'Error',
                description: 'Failed to create row',
                variant: 'destructive',
            });
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
                <Button variant="outline">
                    <Plus className="mr-2 h-4 w-4"/>
                    Add Row Manually
                </Button>
            </DialogTrigger>
            <DialogContent className="max-w-lg">
                <form onSubmit={handleSubmit}>
                    <DialogHeader>
                        <DialogTitle>Add New Row</DialogTitle>
                        <DialogDescription>
                            Manually enter data for a new row in this table.
                        </DialogDescription>
                    </DialogHeader>
                    <div className="grid gap-4 py-4 max-h-[60vh] overflow-y-auto">
                        {columns.map((column) => (
                            <div key={column.id} className="grid gap-2">
                                <Label htmlFor={column.id}>
                                    {column.name}
                                    {!column.optional && <span className="text-destructive ml-1">*</span>}
                                </Label>
                                <Input
                                    id={column.id}
                                    value={values[column.id] || ''}
                                    onChange={(e) =>
                                        setValues({...values, [column.id]: e.target.value})
                                    }
                                    placeholder={column.description}
                                />
                                <p className="text-xs text-muted-foreground">{column.description}</p>
                            </div>
                        ))}
                    </div>
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={() => setOpen(false)}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={isLoading}>
                            {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin"/>}
                            Create Row
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
};

export default CreateRowDialog;
