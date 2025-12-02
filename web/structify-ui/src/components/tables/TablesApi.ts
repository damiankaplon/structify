import {Table} from "./Table.ts";

export interface TablesApi {
    fetchTables(): Promise<Table[]>;
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
}
