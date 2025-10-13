import React from "react";
import {AuthContext} from "./AuthContext.tsx";


export interface AuthProps {
  jwt?: string;
}

export default function AuthProvider(props: { jwtProvider: () => string, children: React.ReactNode}) {
  return (
    <AuthContext.Provider value={{jwt: props.jwtProvider()}}>
      {props.children}
    </AuthContext.Provider>
  );
}


