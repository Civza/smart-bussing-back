-- =============================================================================
-- SMART BUSSING: Migración de Esquema y Sincronización Supabase OAuth
-- Archivo: database/migrations/V1__add_rol_and_supabase_oauth_sync.sql
-- =============================================================================

-- 1. Asegurar la columna 'rol' en la tabla 'public.usuario' con valor por defecto 'USER'
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
          AND table_name = 'usuario' 
          AND column_name = 'rol'
    ) THEN
        ALTER TABLE public.usuario 
        ADD COLUMN rol VARCHAR(20) DEFAULT 'USER' NOT NULL;
    END IF;
END $$;

-- 2. Asegurar restricción de unicidad para 'email'
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'usuario_email_key'
    ) THEN
        ALTER TABLE public.usuario 
        ADD CONSTRAINT usuario_email_key UNIQUE (email);
    END IF;
END $$;

-- 3. Crear o actualizar la función de sincronización con Supabase Auth (auth.users)
CREATE OR REPLACE FUNCTION public.handle_new_user() 
RETURNS trigger
LANGUAGE plpgsql 
SECURITY DEFINER
SET search_path TO 'public'
AS $$
BEGIN
  INSERT INTO public.usuario (
    id_usuario, 
    email, 
    nombre, 
    profile_photourl, 
    rol,
    password
  )
  VALUES (
    nextval('public.usuario_sequence'),
    NEW.email,
    COALESCE(NEW.raw_user_meta_data ->> 'full_name', NEW.email),
    NEW.raw_user_meta_data ->> 'avatar_url',
    'USER',
    'OAUTH_GOOGLE'
  )
  ON CONFLICT (email) DO UPDATE 
  SET 
    nombre = EXCLUDED.nombre,
    profile_photourl = EXCLUDED.profile_photourl;
  RETURN NEW;
END;
$$;

ALTER FUNCTION public.handle_new_user() OWNER TO postgres;

-- 4. Vincular el trigger a la tabla auth.users de Supabase
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 5. Comando de ejemplo para asignar manualmente el rol de ADMIN / Super Usuario:
-- UPDATE public.usuario SET rol = 'ADMIN' WHERE email = 'tu_correo_supervisor@cetys.mx';
