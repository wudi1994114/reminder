# Stage 1: Build the application
FROM docker-0.unsee.tech/node:18-alpine AS build

# Set working directory
WORKDIR /app

# Copy package.json and package-lock.json (or yarn.lock)
COPY package*.json ./
# If you use yarn, uncomment the next line and comment out the npm ci line
# COPY yarn.lock ./

# Install dependencies
# Use npm ci for cleaner installs if package-lock.json is present and up-to-date
RUN npm ci
# If you use yarn, uncomment the next line and comment out the npm ci line
# RUN yarn install --frozen-lockfile

# Copy the rest of the application code
COPY . .

# Build the application for production
RUN npm run build
# If you use yarn, uncomment the next line and comment out the npm run build line
# RUN yarn build

# Stage 2: Serve the application using Nginx
FROM docker-0.unsee.tech/nginx:alpine

# Copy built assets from the build stage
# Assuming the build output is in the 'dist' directory
COPY --from=build /app/dist /usr/share/nginx/html

# Optional: Copy a custom Nginx configuration if you have one
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

# Start Nginx when the container launches
CMD ["nginx", "-g", "daemon off;"] 