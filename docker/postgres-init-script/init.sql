CREATE
    USER structify WITH PASSWORD 'structify';

CREATE
    DATABASE structify OWNER structify;

GRANT ALL PRIVILEGES ON DATABASE
    structify TO structify;

CREATE
    USER keycloak WITH PASSWORD 'keycloak';

CREATE
    DATABASE keycloak OWNER keycloak;

GRANT ALL PRIVILEGES ON DATABASE
    keycloak TO keycloak;

\c structify
CREATE EXTENSION postgis;
