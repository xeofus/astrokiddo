FROM node:22.12 as build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /angular-app/dist/astrokiddo-web/browser/ /usr/share/nginx/html/
EXPOSE 80
CMD ["nginx","-g","daemon off;"]
