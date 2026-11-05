-- Disable RLS for sandbox audit environment so test roles can be audited without table locks
ALTER TABLE "public"."contrato" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."coordenadas" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."coordenadas_ruta" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."empresa" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."interesado" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."lugar" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."parada" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."reporte_ruta" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."ruta" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."ruta_parada" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."usuario" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."viaje" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."viaje_parada" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "public"."viaje_ruta" DISABLE ROW LEVEL SECURITY;

-- Insert Synthetic Test Accounts for Pentesting / Audit Matrix
INSERT INTO "public"."usuario" ("id_usuario", "nombre", "email", "password", "profile_photourl") VALUES
(1, 'Audit Admin', 'admin@audit.com', 'AdminPass123!', 'https://via.placeholder.com/150'),
(2, 'Driver Tester 1', 'driver1@audit.com', 'DriverPass123!', 'https://via.placeholder.com/150'),
(3, 'Driver Tester 2', 'driver2@audit.com', 'DriverPass123!', 'https://via.placeholder.com/150'),
(4, 'Passenger Tester 1', 'passenger1@audit.com', 'PassengerPass123!', 'https://via.placeholder.com/150'),
(5, 'Passenger Tester 2', 'passenger2@audit.com', 'PassengerPass123!', 'https://via.placeholder.com/150')
ON CONFLICT ("id_usuario") DO NOTHING;

SELECT setval('public.usuario_sequence', 10, true);

-- Insert Sample Audit Routes
INSERT INTO "public"."ruta" ("id_ruta", "active", "horario_ruta", "nombre_ruta", "color_ruta", "color_texto_ruta", "nombre_corto_ruta", "tipo_ruta") VALUES
(1, true, '06:00 - 22:00', 'Ruta 1 Sandbox - Centro a Norte', '#FF5733', '#FFFFFF', 'R1', 'Urbana'),
(2, true, '06:30 - 21:30', 'Ruta 2 Sandbox - Poniente a Oriente', '#33C1FF', '#000000', 'R2', 'Directa')
ON CONFLICT ("id_ruta") DO NOTHING;

SELECT setval('public.ruta_sequence', 10, true);

-- Insert Sample Coordinates
INSERT INTO "public"."coordenadas" ("id_coordenada", "latitud", "longitud") VALUES
(1, 19.432608, -99.133209),
(2, 19.434608, -99.135209),
(3, 19.436608, -99.137209)
ON CONFLICT ("id_coordenada") DO NOTHING;

SELECT setval('public.coordenada_sequence', 10, true);
