# ============================================
# MetroHub Frontend - Multi-Stage Dockerfile
# ============================================
# Stage 1: Build React app with Node
# Stage 2: Serve with Nginx Alpine
# ============================================

# ---- Stage 1: Build ----
FROM node:18-alpine AS build

WORKDIR /app

# Copy package files first for dependency layer caching
# Dependencies are re-installed only when package*.json changes
COPY package.json package-lock.json ./
RUN npm install --legacy-peer-deps

# Copy source code
COPY . .

# Build argument for API URL (baked into production bundle at build time)
ARG REACT_APP_API_URL=http://localhost:8080/api
ENV REACT_APP_API_URL=$REACT_APP_API_URL

# Build the production bundle
RUN npm run build

# ---- Stage 2: Serve ----
FROM nginx:1.25-alpine

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom nginx config for SPA routing
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built React app from build stage
COPY --from=build /app/build /usr/share/nginx/html

# Expose port
EXPOSE 80

# Nginx runs as non-root by default in alpine
CMD ["nginx", "-g", "daemon off;"]
