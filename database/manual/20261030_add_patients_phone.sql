-- Cambio aplicado manualmente en producción el 30 de octubre de 2026.
-- La aplicación usa JPA_DDL_AUTO=none; este archivo documenta el cambio.
ALTER TABLE public.patients
    ADD COLUMN IF NOT EXISTS phone varchar(30);
