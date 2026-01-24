import axios, {AxiosInstance} from 'axios';

const API_BASE_URL = '/api';

export const createApiClient = (jwt: string): AxiosInstance => {
    return axios.create({
        baseURL: API_BASE_URL,
        headers: {
            'Authorization': `Bearer ${jwt}`,
            'Content-Type': 'application/json',
        },
    });
};

// Types
export interface Table {
    id: string;
    name: string;
    description?: string;
}

export interface ColumnType {
    type: 'STRING' | 'NUMBER' | 'DATE' | 'BOOLEAN';
    format: string | null;
}

export interface Column {
    id: string;
    name: string;
    description: string;
    type: ColumnType;
    optional: boolean;
}

export interface TableVersion {
    id: string;
    columns: Column[];
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

export interface CreateColumnRequest {
    name: string;
    description: string;
    type: 'STRING' | 'NUMBER' | 'DATE' | 'BOOLEAN';
    stringFormat: string | null;
    optional: boolean;
}

export interface CreateRowRequest {
    cells: {
        columnId: string;
        value: string;
    }[];
}

// API Functions
export const tablesApi = {
    getAll: async (client: AxiosInstance): Promise<Table[]> => {
        const response = await client.get('/tables');
        return response.data;
    },

    create: async (client: AxiosInstance, name: string, description: string): Promise<{ id: string }> => {
        const response = await client.post('/tables', {name, description});
        return response.data;
    },

    getCurrentVersion: async (client: AxiosInstance, tableId: string): Promise<TableVersion | null> => {
        try {
            const response = await client.get(`/tables/${tableId}/versions/current`);
            return response.data;
        } catch (error: any) {
            if (error.response?.status === 404) {
                return null;
            }
            throw error;
        }
    },

    createVersion: async (client: AxiosInstance, tableId: string, columns: CreateColumnRequest[]): Promise<void> => {
        await client.post(`/tables/${tableId}/versions`, columns);
    },

    getRows: async (client: AxiosInstance, tableId: string): Promise<Row[]> => {
        const response = await client.get(`/tables/${tableId}/rows`);
        return response.data;
    },

    createRow: async (client: AxiosInstance, tableId: string, row: CreateRowRequest): Promise<{ id: string }> => {
        const response = await client.post(`/tables/${tableId}/rows`, row);
        return response.data;
    },

    updateRow: async (client: AxiosInstance, tableId: string, rowId: string, row: CreateRowRequest): Promise<void> => {
        await client.put(`/tables/${tableId}/rows/${rowId}`, row);
    },

    generateRowFromPdf: async (client: AxiosInstance, tableId: string, versionNumber: number, file: File): Promise<void> => {
        const formData = new FormData();
        formData.append('file', file);

        await client.post(`/tables/${tableId}/versions/${versionNumber}/rows`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });
    },
};
