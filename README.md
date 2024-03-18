[![Java CI with Maven](https://github.com/arburk/vscp-backend/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/arburk/vscp-backend/actions/workflows/maven.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=arburk_vscp-backend&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=arburk_vscp-backend)

# vscp-backend
VSCP backend server providing REST API and business logic to manage app data of https://github.com/arburk/vscp.

## Local development
### Authentication 
Authentication requires OAuth2 login.
#### Google IDP
A client has been created for testing only: https://console.cloud.google.com/ 
#### Keycloak
For local development and testing a keycloak server can be started via docker to simulate auth.
See https://www.keycloak.org/getting-started/getting-started-docker
```
docker run -p 8081:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:24.0.1 start-dev
```

