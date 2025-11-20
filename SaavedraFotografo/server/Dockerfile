FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev
COPY server.js ./
# .env puede montarse o pasarse por variables de entorno
ENV PORT=5050
EXPOSE 5050
CMD ["npm", "start"]
