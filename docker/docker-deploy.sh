#!/bin/bash

# PUPHAX Hungarian Frontend - Docker Deployment Script
# This script builds and deploys the PUPHAX service with Hungarian frontend

set -e

echo "🏥 PUPHAX Magyar Gyógyszer Kereső - Docker Telepítés"
echo "================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed. Please install Docker first."
    echo "Visit: https://docs.docker.com/get-docker/"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    print_error "Docker Compose is not installed. Please install Docker Compose first."
    echo "Visit: https://docs.docker.com/compose/install/"
    exit 1
fi

print_status "Docker and Docker Compose are available ✓"

# Stop and remove existing containers
print_status "Stopping existing PUPHAX containers..."
docker-compose down --remove-orphans 2>/dev/null || true

# Remove old images (optional)
if [ "$1" = "--clean" ]; then
    print_status "Cleaning up old Docker images..."
    docker rmi puphax-hungarian:latest 2>/dev/null || true
    docker system prune -f
fi

# Build and start the service
print_status "Building PUPHAX Hungarian Frontend Docker image..."
docker-compose build --no-cache

print_status "Starting PUPHAX Hungarian Frontend service..."
docker-compose up -d

# Wait for the service to be healthy
print_status "Waiting for service to be ready..."
sleep 10

# Check if the service is running
if docker-compose ps | grep -q "Up"; then
    print_success "PUPHAX Hungarian Frontend is running!"
    
    # Test the health endpoint
    print_status "Testing health endpoint..."
    if curl -f http://localhost:8080/api/v1/gyogyszerek/egeszseg/gyors -s > /dev/null; then
        print_success "Health check passed ✓"
    else
        print_warning "Health check failed - service might still be starting"
    fi
    
    echo ""
    echo "🎉 PUPHAX Magyar Gyógyszer Kereső telepítve!"
    echo ""
    echo "📱 Frontend URL: http://localhost:8080"
    echo "📋 API Documentation: http://localhost:8080/swagger-ui.html"
    echo "🏥 Health Check: http://localhost:8080/api/v1/gyogyszerek/egeszseg"
    echo ""
    echo "🔧 Hasznos parancsok:"
    echo "  docker-compose logs -f                    # Logok követése"
    echo "  docker-compose restart                    # Szolgáltatás újraindítása"
    echo "  docker-compose down                       # Szolgáltatás leállítása"
    echo "  docker-compose exec puphax-hungarian bash # Konténerbe belépés"
    echo ""
    print_status "A Magyar frontend most elérhető a port 8080-on!"
    
else
    print_error "Failed to start PUPHAX Hungarian Frontend"
    echo ""
    echo "Debug információk:"
    docker-compose logs --tail=50
    exit 1
fi