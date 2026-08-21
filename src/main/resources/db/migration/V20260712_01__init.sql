-- Fase 0: baseline migration. Enables PostGIS so `geography(Point, 4326)` columns
-- (Work.location, Measurement.location) are available starting Fase 2.
CREATE EXTENSION IF NOT EXISTS postgis;
