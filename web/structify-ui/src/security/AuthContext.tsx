import React from "react";
import {AuthProps} from "./AuthProvider.tsx";

export const AuthContext = React.createContext<AuthProps>({jwt: undefined});
