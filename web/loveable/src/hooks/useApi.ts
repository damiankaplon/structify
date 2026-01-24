import {useMemo} from 'react';
import {useAuth} from '@/contexts/AuthContext';
import {createApiClient} from '@/lib/api';

export const useApi = () => {
    const {jwt} = useAuth();

    const client = useMemo(() => createApiClient(jwt), [jwt]);

    return client;
};
