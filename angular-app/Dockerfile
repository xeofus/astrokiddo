
FROM node:20 as build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/astrokiddo-web/ /usr/share/nginx/html
EXPOSE 8092
CMD ["nginx","-g","daemon off;"]
