-- Access4All feature expansion migration.
-- Safe to run multiple times in Supabase SQL editor.

CREATE TABLE IF NOT EXISTS public.event_ticket_tiers (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  event_id uuid NOT NULL,
  name character varying NOT NULL,
  type character varying NOT NULL DEFAULT 'STANDARD',
  price numeric NOT NULL DEFAULT 0,
  capacity integer NOT NULL DEFAULT 0 CHECK (capacity >= 0),
  sales_start_at timestamp without time zone,
  sales_end_at timestamp without time zone,
  active boolean NOT NULL DEFAULT true,
  sort_order integer NOT NULL DEFAULT 0,
  created_at timestamp without time zone DEFAULT now(),
  updated_at timestamp without time zone DEFAULT now(),
  CONSTRAINT event_ticket_tiers_pkey PRIMARY KEY (id),
  CONSTRAINT event_ticket_tiers_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE,
  CONSTRAINT event_ticket_tiers_name_not_blank CHECK (length(TRIM(BOTH FROM name)) > 0),
  CONSTRAINT event_ticket_tiers_price_non_negative CHECK (price >= 0),
  CONSTRAINT event_ticket_tiers_sales_window_check CHECK (sales_start_at IS NULL OR sales_end_at IS NULL OR sales_end_at >= sales_start_at)
);

ALTER TABLE public.registrations
  ADD COLUMN IF NOT EXISTS ticket_tier_id uuid,
  ADD COLUMN IF NOT EXISTS quantity integer NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS unit_price numeric NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS total_price numeric NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS currency character varying NOT NULL DEFAULT 'SGD';

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'registrations_ticket_tier_id_fkey'
      AND conrelid = 'public.registrations'::regclass
  ) THEN
    ALTER TABLE public.registrations
      ADD CONSTRAINT registrations_ticket_tier_id_fkey
      FOREIGN KEY (ticket_tier_id) REFERENCES public.event_ticket_tiers(id);
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'registrations_quantity_positive'
      AND conrelid = 'public.registrations'::regclass
  ) THEN
    ALTER TABLE public.registrations
      ADD CONSTRAINT registrations_quantity_positive CHECK (quantity > 0);
  END IF;
END $$;

ALTER TABLE public.users
  ADD COLUMN IF NOT EXISTS status character varying NOT NULL DEFAULT 'ACTIVE';

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'users_status_check'
      AND conrelid = 'public.users'::regclass
  ) THEN
    ALTER TABLE public.users
      ADD CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'DEACTIVATED'));
  END IF;
END $$;

INSERT INTO public.event_ticket_tiers (event_id, name, type, price, capacity, active, sort_order)
SELECT e.id, 'Standard', 'STANDARD', COALESCE(e.price, 0), COALESCE(e.capacity, 0), true, 0
FROM public.events e
WHERE NOT EXISTS (
  SELECT 1
  FROM public.event_ticket_tiers tier
  WHERE tier.event_id = e.id
);

UPDATE public.registrations r
SET ticket_tier_id = (
  SELECT t.id
  FROM public.event_ticket_tiers t
  WHERE t.event_id = r.event_id
  ORDER BY t.sort_order ASC, t.created_at ASC, t.id ASC
  LIMIT 1
)
WHERE r.ticket_tier_id IS NULL;

UPDATE public.registrations r
SET unit_price = COALESCE(t.price, e.price, 0),
    total_price = COALESCE(t.price, e.price, 0) * GREATEST(COALESCE(r.quantity, 1), 1),
    currency = COALESCE(NULLIF(r.currency, ''), 'SGD')
FROM public.events e
LEFT JOIN public.event_ticket_tiers t ON t.id = r.ticket_tier_id
WHERE e.id = r.event_id;

CREATE INDEX IF NOT EXISTS idx_event_ticket_tiers_event_id ON public.event_ticket_tiers(event_id);
CREATE INDEX IF NOT EXISTS idx_event_ticket_tiers_event_active ON public.event_ticket_tiers(event_id, active, sort_order);
CREATE INDEX IF NOT EXISTS idx_registrations_ticket_tier_id ON public.registrations(ticket_tier_id);
CREATE INDEX IF NOT EXISTS idx_registrations_event_status ON public.registrations(event_id, status);
CREATE INDEX IF NOT EXISTS idx_users_role_status ON public.users(role, status);
