import React, {useEffect, useState} from 'react';
import {Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, TextField} from '@mui/material';
import {useNavigate} from "react-router-dom";

export type CreateTableFormInput = {
    name: string;
    description: string;
};

interface CreateTableDialogProps {
    open: boolean;
    onClose: () => void;
    onSubmit: (input: CreateTableFormInput) => Promise<string | Error>;
}

const CreateTableDialog: React.FC<CreateTableDialogProps> = ({open, onClose, onSubmit}) => {
    const navigate = useNavigate();
    const [creating, setCreating] = useState(false);
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [submitError, setSubmitError] = useState<string | null>(null);

    const closeDialog = () => {
        if (creating) return; // prevent closing while submitting
        onClose();
    };

    useEffect(() => {
        if (open) {
            setSubmitError(null);
            setName('');
            setDescription('');
        }
    }, [open]);

    const handleCreate = async () => {
        const trimmed = name.trim();
        if (!trimmed) {
            setSubmitError('Table name is required.');
            return;
        }
        setSubmitError(null);
        setCreating(true);
        try {
            const result = await onSubmit({name: trimmed, description: description.trim()});
            if (typeof result === 'string') {
                navigate(`/tables/${result}`);
                closeDialog();
            } else {
                setSubmitError(result.message || 'Failed to create table');
            }
        } catch (e) {
            setSubmitError(e instanceof Error ? e.message : 'Failed to create table');
        } finally {
            setCreating(false);
        }
    };

    return (
        <Dialog open={open} onClose={closeDialog} fullWidth maxWidth="sm">
            <DialogTitle>Create new table</DialogTitle>
            <DialogContent>
                <Stack spacing={2} sx={{pt: 1}}>
                    {submitError && <Alert severity="error">{submitError}</Alert>}
                    <TextField
                        label="Table name"
                        required
                        autoFocus
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        disabled={creating}
                    />
                    <TextField
                        label="Description (optional)"
                        multiline
                        minRows={3}
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        disabled={creating}
                    />
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={closeDialog} color="inherit" disabled={creating}>Cancel</Button>
                <Button onClick={handleCreate} variant="contained" disabled={creating || name.trim().length === 0}>
                    {creating ? 'Creating…' : 'Create'}
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default CreateTableDialog;
