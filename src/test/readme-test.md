WARNING: Every time you have to update the db schema, you have to update the schema_backup in database/schema_backup.sql
the contanerized test run on the schema, so to avoid any conflict do the following command while running docker in the background:

npx supabase db dump --db-url "postgresql://[USER]:[PASSWORD]@aws-1-us-east-2.pooler.supabase.com:5432/postgres" -s public -f database/schema_backup.sql       

--- 

# To run the test locally: 

# 1. Start container (runs database with your schema)
docker-compose -f docker-compose-test.yml up -d

# 2. Verify database is accessible
psql -h localhost -p 5433 -U smartbusser  -d smartbuss_test -c "\dt" (should return schema tables)

# 4. Run all tests (unit + integration)
mvn clean test "-Dspring.config.name=application-test"

# 5. View results
# Look for:
# - [INFO] Tests run: X
# - [INFO] BUILD SUCCESS (if all pass)
# - [INFO] BUILD FAILURE (if any fail)

# 6. Stop container when done
docker-compose -f docker-compose-test.yml down
