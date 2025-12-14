import React, {useContext, useEffect, useMemo, useState} from 'react';
import {
    Box,
    Button,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControl,
    Grid,
    IconButton,
    InputLabel,
    MenuItem,
    Select,
    SelectChangeEvent,
    TextField,
    Typography,
    useMediaQuery
} from '@mui/material';
import {useTheme} from '@mui/material/styles';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import SaveIcon from '@mui/icons-material/Save';
import {useParams} from 'react-router-dom';
import {AuthContext} from '../../security/AuthContext';
import {ColumnDto, TablesFetchApi, VersionReadModel} from '../../components/tables/TablesApi';

const DEFAULT_NEW_COLUMN: ColumnDto = {
    name: '',
    description: '',
    type: 'STRING',
    stringFormat: null,
    optional: false,
};

const TableDetailView: React.FC = () => {
    const {tableId} = useParams();
    const auth = useContext(AuthContext);
    const api = useMemo(() => new TablesFetchApi(() => auth.jwt!), [auth.jwt]);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);
    const [saveMessage, setSaveMessage] = useState<string | null>(null);

    const [currentVersion, setCurrentVersion] = useState<VersionReadModel | null>(null);
    const [columns, setColumns] = useState<ColumnDto[]>([]);

    // Description dialog state
    const [descOpen, setDescOpen] = useState(false);
    const [descIndex, setDescIndex] = useState<number | null>(null);
    const [descText, setDescText] = useState('');


    const theme = useTheme();
    const fullScreen = useMediaQuery(theme.breakpoints.down('sm'));

    useEffect(() => {
        if (!tableId) return;
        setLoading(true);
        setError(null);
        (async () => {
            try {
                const v = await api.fetchCurrentVersion(tableId);
                setCurrentVersion(v);
                if (v) {
                    const cols: ColumnDto[] = v.columns.map(c => ({
                        name: c.name,
                        description: c.description,
                        type: (c.type?.type ?? 'STRING').toUpperCase(),
                        stringFormat: c.type?.format ?? null,
                        optional: c.optional,
                    }));
                    setColumns(cols);
                } else {
                    // No version yet – start with an empty editor
                    setColumns([]);
                }
            } catch (e) {
                setError(e instanceof Error ? e.message : 'Failed to load table');
            } finally {
                setLoading(false);
            }
        })();
    }, [api, tableId]);

    const updateColumn = (index: number, patch: Partial<ColumnDto>) => {
        setColumns(prev => prev.map((c, i) => i === index ? {...c, ...patch} : c));
    };

    const addColumn = () => setColumns(prev => [...prev, {...DEFAULT_NEW_COLUMN}]);
    const removeColumn = (index: number) => setColumns(prev => prev.filter((_, i) => i !== index));

    const openDescriptionEditor = (index: number) => {
        setDescIndex(index);
        setDescText(columns[index]?.description ?? '');
        setDescOpen(true);
    };

    const closeDescriptionEditor = () => {
        setDescOpen(false);
        setDescIndex(null);
    };

    const saveDescription = () => {
        if (descIndex !== null) {
            updateColumn(descIndex, {description: descText});
        }
        closeDescriptionEditor();
    };

    const saveNewVersion = async () => {
        if (!tableId) return;
        setSaving(true);
        setSaveMessage(null);
        setError(null);
        try {
            // Changing a row means removing one and adding another at its place.
            // Server treats the submitted list as a new version snapshot.
            await api.createNewVersion(tableId, columns);
            const v = await api.fetchCurrentVersion(tableId);
            setCurrentVersion(v);
            if (v) {
                setColumns(v.columns.map(c => ({
                    name: c.name,
                    description: c.description,
                    type: (c.type?.type ?? 'STRING').toUpperCase(),
                    stringFormat: c.type?.format ?? null,
                    optional: c.optional,
                })));
            }
            setSaveMessage('New version saved successfully.');
        } catch (e) {
            setError(e instanceof Error ? e.message : 'Failed to save new version');
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <Box sx={{display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%'}}>
                <CircularProgress/>
            </Box>
        );
    }

    return (
        <Box sx={{p: 3}}>
            <Typography variant="h5" gutterBottom>
                Table Designer {currentVersion ? `(current version #${currentVersion.orderNumber})` : '(no version yet)'}
            </Typography>
            {!currentVersion && !error && (
                <Typography sx={{mb: 2}} color="text.secondary">
                    No version exists yet. Define columns below and click "Save New Version" to create the first version.
                </Typography>
            )}
            {error && (
                <Typography color="error" sx={{mb: 2}}>Error: {error}</Typography>
            )}
            {saveMessage && (
                <Typography color="success.main" sx={{mb: 2}}>{saveMessage}</Typography>
            )}

            {columns.map((col, idx) => (
                <Grid container spacing={2} key={idx} sx={{mb: 1}} alignItems="center">
                    <Grid item xs={12} md={3}>
                        <TextField
                            size="small"
                            fullWidth
                            label="Name"
                            value={col.name}
                            onChange={(e) => updateColumn(idx, {name: e.target.value})}
                        />
                    </Grid>
                    <Grid item xs={12} md={3}>
                        <TextField
                            size="small"
                            fullWidth
                            label="Description"
                            value={col.description}
                            InputProps={{readOnly: true}}
                            onClick={() => openDescriptionEditor(idx)}
                        />
                    </Grid>
                    <Grid item xs={12} md={2}>
                        <FormControl fullWidth size="small">
                            <InputLabel id={`type-label-${idx}`}>Type</InputLabel>
                            <Select
                                labelId={`type-label-${idx}`}
                                label="Type"
                                value={col.type}
                                onChange={(e: SelectChangeEvent) => {
                                    const newType = e.target.value;
                                    // Reset stringFormat if switching away from STRING
                                    updateColumn(idx, {type: newType, stringFormat: newType === 'STRING' ? col.stringFormat ?? null : null});
                                }}
                            >
                                <MenuItem value="STRING">Text</MenuItem>
                                <MenuItem value="NUMBER">Numeric</MenuItem>
                            </Select>
                        </FormControl>
                    </Grid>
                    <Grid item xs={12} md={2}>
                        <FormControl fullWidth size="small" disabled={col.type !== 'STRING'}>
                            <InputLabel id={`string-format-label-${idx}`}>String Format</InputLabel>
                            <Select
                                labelId={`string-format-label-${idx}`}
                                label="String Format"
                                value={(col.stringFormat ?? '') as string}
                                onChange={(e: SelectChangeEvent<string>) => {
                                    const v = e.target.value;
                                    updateColumn(idx, {stringFormat: v === '' ? null : v});
                                }}
                            >
                                <MenuItem value="">None</MenuItem>
                                <MenuItem value="DATE">DATE</MenuItem>
                            </Select>
                        </FormControl>
                    </Grid>
                    <Grid item xs={12} md={1}>
                        <FormControl fullWidth size="small">
                            <InputLabel id={`optional-label-${idx}`}>Optional</InputLabel>
                            <Select
                                labelId={`optional-label-${idx}`}
                                label="Optional"
                                value={col.optional ? 'true' : 'false'}
                                onChange={(e) => updateColumn(idx, {optional: e.target.value === 'true'})}
                            >
                                <MenuItem value={'false'}>false</MenuItem>
                                <MenuItem value={'true'}>true</MenuItem>
                            </Select>
                        </FormControl>
                    </Grid>
                    <Grid item xs={12} md={1}>
                        <IconButton aria-label="delete" color="error" onClick={() => removeColumn(idx)}>
                            <DeleteIcon/>
                        </IconButton>
                    </Grid>
                </Grid>
            ))}

            <Box sx={{display: 'flex', gap: 1, mt: 2}}>
                <Button startIcon={<AddIcon/>} variant="outlined" onClick={addColumn}>Add Column</Button>
                <Button startIcon={<SaveIcon/>} variant="contained" onClick={saveNewVersion} disabled={saving}>
                    {saving ? 'Saving...' : 'Save New Version'}
                </Button>
            </Box>

            {/* Description Editor Dialog */}
            <Dialog open={descOpen} onClose={closeDescriptionEditor} fullScreen={fullScreen} maxWidth="md" fullWidth>
                <DialogTitle>Edit Description</DialogTitle>
                <DialogContent>
                    <TextField
                        autoFocus
                        margin="dense"
                        label="Description"
                        fullWidth
                        multiline
                        minRows={10}
                        maxRows={30}
                        value={descText}
                        onChange={(e) => setDescText(e.target.value)}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={closeDescriptionEditor} color="inherit">Cancel</Button>
                    <Button onClick={saveDescription} variant="contained">Save</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default TableDetailView;
