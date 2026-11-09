# --- Build stage ---
FROM node:20-alpine AS build
WORKDIR /app

# Trust any extra root CAs dropped into certs/ so `npm install` works on a
# TLS-intercepting network (corporate proxy / antivirus "web shield"). Drop the
# intercepting root as a PEM into frontend/certs/. No-op on a clean checkout
# (certs/ holds only .gitkeep), where Node falls back to its bundled CAs.
COPY certs/ /tmp/certs/
RUN cat /tmp/certs/*.pem > /tmp/extra-ca.pem 2>/dev/null || touch /tmp/extra-ca.pem
ENV NODE_EXTRA_CA_CERTS=/tmp/extra-ca.pem

COPY package.json package-lock.json* ./
RUN npm install
COPY . .
RUN npm run build

# --- Serve stage ---
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
