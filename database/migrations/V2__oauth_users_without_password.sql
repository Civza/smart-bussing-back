-- =============================================================================
-- SMART BUSSING: Cuentas de Google sin contrasena local (KAN-16 / T2)
-- Archivo: database/migrations/V2__oauth_users_without_password.sql
--
-- Antes: el trigger guardaba la contrasena constante 'OAUTH_GOOGLE' y /user/login
-- la comparaba en texto plano, asi que cualquiera podia entrar como una cuenta de
-- Google (incluido el ADMIN) usando esa palabra. El backend ya la rechaza desde la
-- rama feature/auth; esta migracion limpia los datos para que no quede ningun rastro.
--
-- Se aplica A MANO (ver ticket de Jira). Idempotente: se puede ejecutar varias veces.
-- Las contrasenas en texto plano de cuentas locales NO se tocan aqui: el backend las
-- re-hashea con BCrypt en el siguiente login exitoso.
-- =============================================================================

BEGIN;

-- 1. Las cuentas de Google no tienen contrasena local.
UPDATE public.usuario
SET password = NULL
WHERE password = 'OAUTH_GOOGLE';

-- 2. El trigger de sincronizacion deja de escribir la contrasena constante.
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
    NULL
  )
  ON CONFLICT (email) DO UPDATE
  SET
    nombre = EXCLUDED.nombre,
    profile_photourl = EXCLUDED.profile_photourl;
  RETURN NEW;
END;
$$;

COMMIT;

-- Verificacion (debe devolver 0):
-- SELECT count(*) FROM public.usuario WHERE password = 'OAUTH_GOOGLE';
