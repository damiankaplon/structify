import React, {useState} from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {Button} from '@/components/ui/button';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {Checkbox} from '@/components/ui/checkbox';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {Plus, Trash2, Loader2} from 'lucide-react';
import {useApi} from '@/hooks/useApi';
import {tablesApi, CreateColumnRequest, Column} from '@/lib/api';
import {useToast} from '@/hooks/use-toast';
import {Card} from '@/components/ui/card';

interface ColumnFormData {
  id: string;
  name: string;
  description: string;
  type: 'STRING' | 'NUMBER' | 'DATE' | 'BOOLEAN' | 'OBJECT';
  optional: boolean;
  children: ColumnFormData[];
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
  children: [],
});

const mapColumnToForm = (col: Column): ColumnFormData => ({
  id: col.id,
  name: col.name,
  description: col.description,
  type: col.type.type,
  optional: col.optional,
  children: col.children?.map(mapColumnToForm) || [],
});

const mapFormToRequest = (col: ColumnFormData): CreateColumnRequest => ({
  name: col.name,
  description: col.description,
  type: col.type,
  stringFormat: null,
  optional: col.optional,
  children: col.type === 'OBJECT' ? col.children.map(mapFormToRequest) : undefined,
});

interface ColumnEditorProps {
  column: ColumnFormData;
  index: number;
  depth: number;
  onUpdate: (id: string, updates: Partial<ColumnFormData>) => void;
  onRemove: (id: string) => void;
  canRemove: boolean;
}

const ColumnEditor: React.FC<ColumnEditorProps> = ({
                                                     column,
                                                     index,
                                                     depth,
                                                     onUpdate,
                                                     onRemove,
                                                     canRemove,
                                                   }) => {
  const addChild = () => {
    onUpdate(column.id, {children: [...column.children, createEmptyColumn()]});
  };

  const removeChild = (childId: string) => {
    onUpdate(column.id, {children: column.children.filter((c) => c.id !== childId)});
  };

  const updateChild = (childId: string, updates: Partial<ColumnFormData>) => {
    onUpdate(column.id, {
      children: column.children.map((c) => (c.id === childId ? {...c, ...updates} : c)),
    });
  };

  const handleTypeChange = (newType: string) => {
    const newChildren = newType === 'OBJECT'
        ? (column.children.length === 0 ? [createEmptyColumn()] : column.children)
        : [];
    onUpdate(column.id, {type: newType as ColumnFormData['type'], children: newChildren});
  };

  return (
      <Card className="p-4" style={{marginLeft: depth > 0 ? `${depth * 1.5}rem` : 0}}>
        <div className="flex items-start gap-4">
          <div className="flex items-center pt-2 text-muted-foreground">
            <span className="text-sm font-medium">{index + 1}</span>
          </div>
          <div className="flex-1 grid gap-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Column Name</Label>
                <Input
                    value={column.name}
                    onChange={(e) => onUpdate(column.id, {name: e.target.value})}
                    placeholder="e.g., First Name"
                />
              </div>
              <div className="space-y-2">
                <Label>Type</Label>
                <Select value={column.type} onValueChange={handleTypeChange}>
                  <SelectTrigger>
                    <SelectValue/>
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="STRING">Text</SelectItem>
                    <SelectItem value="NUMBER">Number</SelectItem>
                    <SelectItem value="DATE">Date</SelectItem>
                    <SelectItem value="BOOLEAN">Yes/No</SelectItem>
                    <SelectItem value="OBJECT">Object (nested)</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="space-y-2">
              <Label>Description</Label>
              <Textarea
                  value={column.description}
                  onChange={(e) => onUpdate(column.id, {description: e.target.value})}
                  placeholder="Describe what this column should contain."
                  rows={2}
              />
            </div>
            <div className="flex items-center space-x-2">
              <Checkbox
                  id={`optional-${column.id}`}
                  checked={column.optional}
                  onCheckedChange={(checked) => onUpdate(column.id, {optional: checked as boolean})}
              />
              <Label htmlFor={`optional-${column.id}`} className="text-sm font-normal">
                Optional
              </Label>
            </div>

            {column.type === 'OBJECT' && (
                <div className="space-y-3 border-l-2 border-muted pl-4 mt-2">
                  <Label className="text-xs text-muted-foreground uppercase tracking-wider">
                    Child Columns
                  </Label>
                  {column.children.map((child, childIndex) => (
                      <ColumnEditor
                          key={child.id}
                          column={child}
                          index={childIndex}
                          depth={0}
                          onUpdate={updateChild}
                          onRemove={removeChild}
                          canRemove={column.children.length > 1}
                      />
                  ))}
                  <Button type="button" variant="outline" size="sm" onClick={addChild}>
                    <Plus className="mr-2 h-3 w-3"/>
                    Add Child Column
                  </Button>
                </div>
            )}
          </div>
          <Button
              type="button"
              variant="ghost"
              size="icon"
              onClick={() => onRemove(column.id)}
              disabled={!canRemove}
              className="text-muted-foreground hover:text-destructive"
          >
            <Trash2 className="h-4 w-4"/>
          </Button>
        </div>
      </Card>
  );
};

const EditColumnsDialog: React.FC<EditColumnsDialogProps> = ({
                                                               open,
                                                               onOpenChange,
                                                               tableId,
                                                               existingColumns,
                                                               onVersionCreated,
                                                             }) => {
  const [columns, setColumns] = useState<ColumnFormData[]>(() => {
    if (existingColumns && existingColumns.length > 0) {
      return existingColumns.map(mapColumnToForm);
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

  const updateColumn = (id: string, updates: Partial<ColumnFormData>) => {
    setColumns(
        columns.map((col) => (col.id === id ? {...col, ...updates} : col))
    );
  };

  const validateColumns = (cols: ColumnFormData[]): boolean => {
    for (const col of cols) {
      if (!col.name.trim() || !col.description.trim()) return false;
      if (col.type === 'OBJECT' && col.children.length > 0) {
        if (!validateColumns(col.children)) return false;
      }
    }
    return true;
  };

  const handleSubmit = async () => {
    if (!validateColumns(columns)) {
      toast({
        title: 'Validation Error',
        description: 'All columns must have a name and description',
        variant: 'destructive',
      });
      return;
    }

    setIsLoading(true);
    try {
      const columnRequests = columns.map(mapFormToRequest);
      await tablesApi.createVersion(api, tableId, columnRequests);
      toast({title: 'Success', description: 'Table structure updated successfully'});
      onOpenChange(false);
      onVersionCreated();
    } catch (error) {
      toast({title: 'Error', description: 'Failed to update table structure', variant: 'destructive'});
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
              Define columns for this table. Use "Object" type to create nested column groups.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            {columns.map((column, index) => (
                <ColumnEditor
                    key={column.id}
                    column={column}
                    index={index}
                    depth={0}
                    onUpdate={updateColumn}
                    onRemove={removeColumn}
                    canRemove={columns.length > 1}
                />
            ))}
            <Button type="button" variant="outline" onClick={addColumn} className="w-full">
              <Plus className="mr-2 h-4 w-4"/>
              Add Column
            </Button>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
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
