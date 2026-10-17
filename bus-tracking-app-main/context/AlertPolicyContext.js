import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { useAuth } from './AuthContext';
import { DEFAULT_POLICY, POLICY_FIELDS, validatePolicy } from '../utils/alertPolicy';
import { getAlertPolicy, saveAlertPolicy } from '../services/alertPolicyService';
import { errorMessage } from '../services/api';

const Context = createContext();
export const useAlertPolicy = () => useContext(Context);
export function AlertPolicyProvider({ children }) {
  const { user } = useAuth();
  const [policy, setPolicy] = useState(DEFAULT_POLICY);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [loaded, setLoaded] = useState(false);
  const identity = useRef(user?.id);
  identity.current = user?.id;
  const refreshPolicy = useCallback(async () => {
    if (!user?.id) return;
    setLoading(true);
    try {
      const data = await getAlertPolicy();
      if (identity.current !== user.id) return;
      setPolicy(data); setLoaded(true); setError(null);
    } catch (e) {
      if (identity.current === user.id) setError(errorMessage(e));
    } finally { if (identity.current === user.id) setLoading(false); }
  }, [user?.id]);
  useEffect(() => {
    setPolicy(DEFAULT_POLICY); setLoaded(false); setError(null); setLoading(false);
    refreshPolicy();
  }, [refreshPolicy]);
  const savePolicy = async (draft) => {
    const validation = validatePolicy(draft);
    if (validation) throw new Error(validation);
    const ownerId = user?.id;
    const data = await saveAlertPolicy(Object.fromEntries(POLICY_FIELDS.map(key => [key, Number(draft[key])])));
    if (identity.current === ownerId) { setPolicy(data); setLoaded(true); setError(null); }
  };
  return <Context.Provider value={{ policy, loading, error, loaded, refreshPolicy, savePolicy }}>{children}</Context.Provider>;
}
