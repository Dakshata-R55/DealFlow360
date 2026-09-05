# Dealflow360 UI image. VITE_API_URL empty → browser uses relative /api (Vite preview proxy).
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm install
COPY . .
ARG VITE_API_URL=
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

FROM node:22-alpine
LABEL org.opencontainers.image.title="dealflow360-frontend"
WORKDIR /app
COPY --from=build /app/package.json /app/package-lock.json ./
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
COPY --from=build /app/vite.config.ts ./
ENV DOCKER=1
EXPOSE 5178
CMD ["npm", "run", "preview"]
