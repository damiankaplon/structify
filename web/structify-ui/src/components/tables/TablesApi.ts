import {Table} from "./Table.ts";

export interface ColumnTypeReadModel {
    type: string;
    format?: string | null;
}

export interface ColumnDefinitionReadModel {
    id: number;
    name: string;
    description: string;
    type: ColumnTypeReadModel;
    optional: boolean;
}

export interface VersionReadModel {
    id: string;
    columns: ColumnDefinitionReadModel[];
    orderNumber: number;
}

export type ColumnDto = {
    name: string;
    description: string;
    type: string; // "STRING" | "NUMBER"
    stringFormat?: string | null;
    optional: boolean;
}

export interface TablesApi {
    fetchTables(): Promise<Table[]>;

    fetchCurrentVersion(tableId: string): Promise<VersionReadModel | null>;

    createNewVersion(tableId: string, columns: ColumnDto[]): Promise<void>;

    createTable(name: string, description: string): Promise<string>; // returns newly created table id
}

export class TablesFetchApi implements TablesApi {
    private readonly jwtProvider: () => string;

    constructor(jwtProvider: () => string) {
        this.jwtProvider = jwtProvider;
    }

    async fetchTables(): Promise<Table[]> {
        const jwt = this.jwtProvider();
        const response = await fetch('/api/tables', {
            headers: {
                'Authorization': `Bearer ${jwt}`
            }
        });
        if (!response.ok) {
            throw new Error(`Failed to fetch tables: ${response.statusText}`);
        }
        return await response.json();
    }

    async fetchCurrentVersion(tableId: string): Promise<VersionReadModel | null> {
        const jwt = this.jwtProvider();
        const response = await fetch(`/api/tables/${tableId}/versions/current`, {
            headers: {
                'Authorization': `Bearer ${jwt}`
            }
        });
        if (!response.ok) {
            throw new Error(`Failed to fetch current version: ${response.statusText}`);
        }
        return await response.json(); // may be null when no version exists yet
    }

    async createNewVersion(tableId: string, columns: ColumnDto[]): Promise<void> {
        const jwt = this.jwtProvider();
        const response = await fetch(`/api/tables/${tableId}/versions`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwt}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(columns)
        });
        if (!response.ok) {
            throw new Error(`Failed to create new version: ${response.statusText}`);
        }
    }

    async createTable(name: string, description: string): Promise<string> {
        const jwt = this.jwtProvider();
        const response = await fetch('/api/tables', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwt}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({name, description})
        });
        if (!response.ok) {
            throw new Error(`Failed to create table: ${response.statusText}`);
        }
        const body: { id: string } = await response.json();
        return body.id;
    }
}
