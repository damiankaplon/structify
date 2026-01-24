import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
    url: "http://localhost:8282",
    realm: "structify",
    clientId: "structify_ui_web"
});

export default keycloak;
