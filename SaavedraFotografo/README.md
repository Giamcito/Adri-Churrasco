# Ejecución con Docker

Para levantar toda la aplicación (MySQL, backend Spring Boot y frontend Angular) usa Docker Compose:

```powershell
docker compose build
docker compose up -d
```

Servicios expuestos:
- Frontend: http://localhost (puerto 80)
- Backend API: http://localhost:8080
- MySQL: puerto 3306 (root/1234)

Para ver logs:
```powershell
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
```

Para detener y eliminar contenedores:
```powershell
docker compose down
```

Si deseas cambiar credenciales de base de datos, ajusta variables en `docker-compose.yml` y las de conexión sobreescriben las de `application.properties`.

Para reconstruir después de cambios en código:
```powershell
docker compose build --no-cache backend frontend
docker compose up -d
```

Optimización opcional: activar proxy desde Nginx al backend (descomenta bloque `/api/` en `nginx.conf` y luego apunta las llamadas de Angular a rutas relativas `/api/auth/...`).

# SaavedraFotografo

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 18.2.8.

## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via a platform of your choice. To use this command, you need to first add a package that implements end-to-end testing capabilities.

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
