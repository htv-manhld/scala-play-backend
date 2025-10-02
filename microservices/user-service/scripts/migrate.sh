#!/bin/bash
# Migration script - giống php artisan migrate

set -e

DB_HOST=${DB_HOST:-user-db}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-userdb}
DB_USER=${DB_USER:-postgres}
DB_PASSWORD=${DB_PASSWORD:-password}

EVOLUTIONS_DIR="/app/conf/evolutions/default"

echo "🚀 Running migrations..."

# Kiểm tra connection
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT 1" > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ Cannot connect to database"
    exit 1
fi

# Chạy từng evolution file
for file in $EVOLUTIONS_DIR/*.sql; do
    if [ -f "$file" ]; then
        filename=$(basename $file)
        echo "📝 Applying $filename..."

        # Tạo file temp chỉ chứa phần Ups (bỏ comments và Downs)
        temp_file=$(mktemp)

        # Extract SQL từ # --- !Ups đến # --- !Downs
        awk '/# --- !Ups/,/# --- !Downs/' "$file" | \
            grep -v "^#" | \
            grep -v "^$" > "$temp_file"

        # Chạy SQL
        PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$temp_file" 2>&1 | \
            grep -v "ERROR.*already exists" || true

        rm "$temp_file"
        echo "✅ Applied $filename"
    fi
done

echo "✨ All migrations completed!"
