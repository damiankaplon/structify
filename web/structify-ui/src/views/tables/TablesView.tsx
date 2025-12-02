import React, {useContext} from 'react';
import TablesGrid from '../../components/tables/TablesGrid';
import {TablesFetchApi} from '../../components/tables/TablesApi';
import {AuthContext} from '../../security/AuthContext';

const TablesView: React.FC = () => {
    const authContext = useContext(AuthContext);
    const tablesApi = new TablesFetchApi(() => authContext.jwt!);
    return (
        <div>
            <TablesGrid tablesApi={tablesApi}/>
        </div>
    );
};

export default TablesView;
