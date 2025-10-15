#!/bin/bash
# Migration script with tracking
set -e

# Database connection (can run from host or container)
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-userdb}
DB_USER=${DB_USER:-postgres}
DB_PASSWORD=${DB_PASSWORD:-password}

# Evolution directory (detect if running from host or container)
if [ -d "/app/conf/evolutions/default" ]; then
    EVOLUTIONS_DIR="/app/conf/evolutions/default"
else
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    EVOLUTIONS_DIR="$SCRIPT_DIR/../conf/evolutions/default"
fi

echo "🚀 Running migrations..."

# check connection
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT 1" > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ Cannot connect to database"
    exit 1
fi

# run each evolution file in order
for file in $(ls $EVOLUTIONS_DIR/*.sql | sort -V); do
    if [ -f "$file" ]; then
        filename=$(basename $file)

        # check if migration has already been applied
        already_applied=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c \
            "SELECT COUNT(*) FROM play_evolutions WHERE migration = '$filename';" 2>/dev/null || echo "0")

        # if play_evolutions table doesn't exist, the first migration will create it
        if [ $already_applied -gt 0 ]; then
            echo "⏭️  Skipping $filename (already applied)"
            continue
        fi

        echo "📝 Applying $filename..."

        # create temp file only contains Ups (remove comments and Downs)
        temp_file=$(mktemp)

        # Extract SQL from # --- !Ups to # --- !Downs
        awk '/# --- !Ups/,/# --- !Downs/' "$file" | \
            grep -v "^#" | \
            grep -v "^$" > "$temp_file"

        # run SQL
        if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$temp_file" 2>&1; then
            # Mark migration as applied (if play_evolutions table already exists)
            PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c \
                "INSERT INTO play_evolutions (migration) VALUES ('$filename') ON CONFLICT (migration) DO NOTHING;" \
                2>/dev/null || true

            echo "✅ Applied $filename"
        else
            echo "❌ Failed to apply $filename"
            rm "$temp_file"
            exit 1
        fi

        rm "$temp_file"
    fi
done

echo "✨ All migrations completed!"

# show applied migrations
echo ""
echo "📊 Applied migrations:"
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c \
    "SELECT id, migration, batch, applied_at FROM play_evolutions ORDER BY id;" 2>/dev/null || echo "   (tracking table not yet created)"
