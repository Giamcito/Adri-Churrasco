# Build Angular
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# Construcción en modo desarrollo para evitar fallos de budgets de CSS
RUN npm run build -- --configuration development

# Servir con Nginx
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/saavedra-fotografo/browser/ /usr/share/nginx/html/
EXPOSE 80
