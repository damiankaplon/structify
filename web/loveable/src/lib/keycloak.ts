import Keycloak from 'keycloak-js';

const keycloakUrl = import.meta.env.MODE === 'development'
    ? "http://localhost:8282"
    : "https://structify-auth.damiankaplon.site/";

const keycloak = new Keycloak({
    url: keycloakUrl,
    realm: "structify",
    clientId: "structify_ui_web"
});

export default keycloak;
