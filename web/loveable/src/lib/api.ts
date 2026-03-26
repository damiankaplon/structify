const BASE = '/api';

async function request<T>(
    path: string,
    token: string,
    options: RequestInit = {}
): Promise<T> {
    const res = await fetch(`${BASE}${path}`, {
        ...options,
        headers: {
            ...(options.headers || {}),
            Authorization: `Bearer ${token}`,
            ...(!options.body || options.body instanceof FormData
                ? {}
                : {'Content-Type': 'application/json'}),
        },
    });
    if (!res.ok) {
        const text = await res.text();
        throw new Error(`API ${res.status}: ${text}`);
    }
    const contentLength = res.headers.get('content-length');
    if (contentLength === '0' || res.status === 204) return undefined as T;
    return res.json();
}

// --- Types ---

export interface TableSummary {
    id: string;
    name: string;
    description: string;
}

export interface ColumnType {
    type: 'STRING' | 'NUMBER' | 'OBJECT';
    format: string | null;
}

export interface ColumnDefinition {
    id: string;
    name: string;
    description: string;
    type: ColumnType;
    optional: boolean;
    children: ColumnDefinition[];
}

export interface TableVersion {
    id: string;
    columns: ColumnDefinition[];
    orderNumber: number;
}

export interface Cell {
    columnDefinitionId: string;
    value: string;
}

export interface Row {
    id: string;
    cells: Cell[];
}

// Column definition for creating a version
export interface ColumnInput {
    name: string;
    description: string;
    type: 'STRING' | 'NUMBER' | 'OBJECT';
    stringFormat?: string;
    optional?: boolean;
    children?: ColumnInput[];
}

// --- API functions ---

export const getTables = (token: string) =>
    request<TableSummary[]>('/tables', token);

export const createTable = (token: string, body: { name: string; description: string }) =>
    request<{ id: string }>('/tables', token, {
        method: 'POST',
        body: JSON.stringify(body),
    });

export const getCurrentVersion = (token: string, tableId: string) =>
    request<TableVersion>(`/tables/${tableId}/versions/current`, token);

export const createVersion = (token: string, tableId: string, columns: ColumnInput[]) =>
    request<void>(`/tables/${tableId}/versions`, token, {
        method: 'POST',
        body: JSON.stringify(columns),
    });

export const getRows = (token: string, tableId: string) =>
    request<Row[]>(`/tables/${tableId}/rows`, token);

export const createRow = (
    token: string,
    tableId: string,
    cells: { columnId: string; value: string | number }[]
) =>
    request<{ id: string }>(`/tables/${tableId}/rows`, token, {
        method: 'POST',
        body: JSON.stringify({cells}),
    });

export const updateRow = (
    token: string,
    tableId: string,
    rowId: string,
    cells: { columnId: string; value: string | number }[]
) =>
    request<void>(`/tables/${tableId}/rows/${rowId}`, token, {
        method: 'PUT',
        body: JSON.stringify({cells}),
    });

export const generateRowFromPdf = (
    token: string,
    tableId: string,
    versionOrderNr: number,
    file: File
) => {
    const form = new FormData();
    form.append('file', file);
    return request<{ id: string }>(
        `/tables/${tableId}/versions/${versionOrderNr}/rows`,
        token,
        {method: 'POST', body: form}
    );
};
