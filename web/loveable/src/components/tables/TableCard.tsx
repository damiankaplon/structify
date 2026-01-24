import React from 'react';
import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';
import {Table} from '@/lib/api';
import {Table2, ChevronRight} from 'lucide-react';
import {Link} from 'react-router-dom';

interface TableCardProps {
    table: Table;
}

const TableCard: React.FC<TableCardProps> = ({table}) => {
    return (
        <Link to={`/tables/${table.id}`}>
            <Card className="cursor-pointer transition-all hover:border-primary/50 hover:shadow-md">
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                    <CardTitle className="text-lg font-medium">
                        <div className="flex items-center gap-2">
                            <Table2 className="h-5 w-5 text-muted-foreground"/>
                            {table.name}
                        </div>
                    </CardTitle>
                    <ChevronRight className="h-5 w-5 text-muted-foreground"/>
                </CardHeader>
                <CardContent>
                    <p className="text-sm text-muted-foreground line-clamp-2">
                        {table.description || 'No description provided'}
                    </p>
                </CardContent>
            </Card>
        </Link>
    );
};

export default TableCard;
