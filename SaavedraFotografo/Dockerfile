# Build Angular
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# Construcción en modo producción
RUN npm run build -- --configuration production

# Servir con Nginx
FROM nginx:1.27-alpine
# Config custom para SPA
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Copiar artefactos construidos (según angular.json outputPath)
# Copiar el contenido de la carpeta `browser` (algunas builds generan un subdir `browser`)
COPY --from=build /app/dist/saavedra-fotografo/browser/ /usr/share/nginx/html/
EXPOSE 80
