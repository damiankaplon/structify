export interface Row {
    id: string;
    cells: Cell[];
}

export interface Cell {
    columnId: number;
    value: string;
}
