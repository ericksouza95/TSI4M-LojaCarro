# Frontend Angular servido pelo Nginx, que também encaminha /api para o backend
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/loja-carro-web/browser /usr/share/nginx/html
EXPOSE 80
