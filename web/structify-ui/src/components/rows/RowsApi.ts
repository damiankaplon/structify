import {Row} from './Row.ts';

export interface RowsApi {
    fetchRows(tableId: string): Promise<Row[]>;
}

export class RowsFetchApi implements RowsApi {
    private readonly jwtProvider: () => string;

    constructor(jwtProvider: () => string) {
        this.jwtProvider = jwtProvider;
    }

    async fetchRows(tableId: string): Promise<Row[]> {
        const response = await fetch(`/api/tables/${tableId}/rows`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${this.jwtProvider()}`
            },
        });

        if (!response.ok) {
            throw new Error(`Failed to fetch rows: ${response.statusText}`);
        }

        return await response.json();
    }
}
