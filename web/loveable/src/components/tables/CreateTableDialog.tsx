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
import {Textarea} from '@/components/ui/textarea';
import {Plus, Loader2} from 'lucide-react';
import {useApi} from '@/hooks/useApi';
import {tablesApi} from '@/lib/api';
import {useToast} from '@/hooks/use-toast';

interface CreateTableDialogProps {
    onTableCreated: () => void;
}

const CreateTableDialog: React.FC<CreateTableDialogProps> = ({onTableCreated}) => {
    const [open, setOpen] = useState(false);
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const api = useApi();
    const {toast} = useToast();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!name.trim()) {
            toast({
                title: 'Error',
                description: 'Please enter a table name',
                variant: 'destructive',
            });
            return;
        }

        setIsLoading(true);
        try {
            await tablesApi.create(api, name, description);
            toast({
                title: 'Success',
                description: 'Table created successfully',
            });
            setOpen(false);
            setName('');
            setDescription('');
            onTableCreated();
        } catch (error) {
            toast({
                title: 'Error',
                description: 'Failed to create table',
                variant: 'destructive',
            });
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
                <Button>
                    <Plus className="mr-2 h-4 w-4"/>
                    Create Table
                </Button>
            </DialogTrigger>
            <DialogContent>
                <form onSubmit={handleSubmit}>
                    <DialogHeader>
                        <DialogTitle>Create New Table</DialogTitle>
                        <DialogDescription>
                            Create a new table to define your data structure for PDF extraction.
                        </DialogDescription>
                    </DialogHeader>
                    <div className="grid gap-4 py-4">
                        <div className="grid gap-2">
                            <Label htmlFor="name">Name</Label>
                            <Input
                                id="name"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="e.g., Employee Records"
                            />
                        </div>
                        <div className="grid gap-2">
                            <Label htmlFor="description">Description</Label>
                            <Textarea
                                id="description"
                                value={description}
                                onChange={(e) => setDescription(e.target.value)}
                                placeholder="Describe what data this table will contain..."
                                rows={3}
                            />
                        </div>
                    </div>
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={() => setOpen(false)}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={isLoading}>
                            {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin"/>}
                            Create
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
};

export default CreateTableDialog;
