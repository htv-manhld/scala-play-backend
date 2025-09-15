#!/bin/bash

# Docker helper scripts for Scala Play Backend

set -e

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

# Function to build the Docker image
build_image() {
    print_status "Building Docker image..."
    docker build -f docker/Dockerfile -t scala-play-backend:latest .
    print_success "Docker image built successfully!"
}

# Function to run the application
run_app() {
    print_status "Starting Scala Play Backend application..."
    docker compose up -d scala-play-app
    print_success "Application started! Access it at http://localhost:9000"
}

# Function to run with nginx (production mode)
run_production() {
    print_status "Starting application in production mode with nginx..."
    docker compose --profile production up -d
    print_success "Application started in production mode! Access it at http://localhost"
}

# Function to stop the application
stop_app() {
    print_status "Stopping application..."
    docker compose down
    print_success "Application stopped!"
}

# Function to view logs
view_logs() {
    print_status "Viewing application logs..."
    docker compose logs -f scala-play-app
}

# Function to clean up
cleanup() {
    print_status "Cleaning up Docker resources..."
    docker compose down -v
    docker system prune -f
    print_success "Cleanup completed!"
}

# Function to rebuild and restart
rebuild() {
    print_status "Rebuilding and restarting application..."
    docker compose down
    docker compose build --no-cache
    docker compose up -d
    print_success "Application rebuilt and restarted!"
}

# Function to show help
show_help() {
    echo "Docker Helper Scripts for Scala Play Backend"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  build       Build the Docker image"
    echo "  run         Run the application (development mode)"
    echo "  production  Run the application with nginx (production mode)"
    echo "  stop        Stop the application"
    echo "  logs        View application logs"
    echo "  cleanup     Clean up Docker resources"
    echo "  rebuild     Rebuild and restart the application"
    echo "  help        Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 build"
    echo "  $0 run"
    echo "  $0 production"
    echo "  $0 logs"
}

# Main script logic
case "${1:-help}" in
    build)
        build_image
        ;;
    run)
        run_app
        ;;
    production)
        run_production
        ;;
    stop)
        stop_app
        ;;
    logs)
        view_logs
        ;;
    cleanup)
        cleanup
        ;;
    rebuild)
        rebuild
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "Unknown command: $1"
        show_help
        exit 1
        ;;
esac
