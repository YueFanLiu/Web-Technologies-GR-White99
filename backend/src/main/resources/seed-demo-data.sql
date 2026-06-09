-- Access4All current database seed/schema snapshot.
-- Use this file as the single SQL reference for the current project database.

CREATE TABLE public.users (
  id uuid NOT NULL,
  email character varying NOT NULL UNIQUE,
  full_name character varying NOT NULL,
  phone character varying,
  role character varying NOT NULL CHECK (role::text = ANY (ARRAY['PARENT'::character varying, 'ORGANIZER'::character varying, 'ADMIN'::character varying]::text[])),
  created_at timestamp without time zone DEFAULT now(),
  updated_at timestamp without time zone DEFAULT now(),
  photo text,
  status character varying NOT NULL DEFAULT 'ACTIVE'::character varying CHECK (status::text = ANY (ARRAY['ACTIVE'::character varying, 'DEACTIVATED'::character varying]::text[])),
  CONSTRAINT users_pkey PRIMARY KEY (id),
  CONSTRAINT users_id_fkey FOREIGN KEY (id) REFERENCES auth.users(id)
);
CREATE TABLE public.locations (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  name character varying NOT NULL,
  description text,
  address text,
  city character varying,
  country character varying,
  latitude numeric,
  longitude numeric,
  created_at timestamp without time zone DEFAULT now(),
  updated_at timestamp without time zone DEFAULT now(),
  recommendation_score double precision NOT NULL DEFAULT 0,
  recommendation_score_updated_at timestamp without time zone,
  CONSTRAINT locations_pkey PRIMARY KEY (id)
);
CREATE TABLE public.location_images (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  location_id uuid NOT NULL,
  image_url text NOT NULL,
  created_at timestamp without time zone DEFAULT now(),
  CONSTRAINT location_images_pkey PRIMARY KEY (id),
  CONSTRAINT location_images_location_id_fkey FOREIGN KEY (location_id) REFERENCES public.locations(id)
);
CREATE TABLE public.events (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  organizer_id uuid NOT NULL,
  location_id uuid,
  title character varying NOT NULL,
  description text,
  category character varying,
  start_time timestamp without time zone NOT NULL,
  end_time timestamp without time zone NOT NULL,
  capacity integer NOT NULL CHECK (capacity >= 0),
  price numeric DEFAULT 0,
  is_virtual boolean DEFAULT false,
  status character varying DEFAULT 'PUBLISHED'::character varying,
  created_at timestamp without time zone DEFAULT now(),
  updated_at timestamp without time zone DEFAULT now(),
  recommendation_score double precision NOT NULL DEFAULT 0,
  recommendation_score_updated_at timestamp without time zone,
  CONSTRAINT events_pkey PRIMARY KEY (id),
  CONSTRAINT events_organizer_id_fkey FOREIGN KEY (organizer_id) REFERENCES public.users(id),
  CONSTRAINT events_location_id_fkey FOREIGN KEY (location_id) REFERENCES public.locations(id)
);
CREATE TABLE public.event_images (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  event_id uuid NOT NULL,
  image_url text NOT NULL,
  created_at timestamp without time zone DEFAULT now(),
  CONSTRAINT event_images_pkey PRIMARY KEY (id),
  CONSTRAINT event_images_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id)
);
CREATE TABLE public.registrations (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  event_id uuid NOT NULL,
  user_id uuid NOT NULL,
  status character varying DEFAULT 'CONFIRMED'::character varying,
  registered_at timestamp without time zone DEFAULT now(),
  contact_full_name character varying CHECK (contact_full_name IS NULL OR length(TRIM(BOTH FROM contact_full_name)) > 0),
  contact_email character varying CHECK (contact_email IS NULL OR length(TRIM(BOTH FROM contact_email)) > 0),
  contact_phone character varying CHECK (contact_phone IS NULL OR length(TRIM(BOTH FROM contact_phone)) > 0),
  ticket_tier_id uuid,
  quantity integer NOT NULL DEFAULT 1 CHECK (quantity > 0),
  unit_price numeric NOT NULL DEFAULT 0,
  total_price numeric NOT NULL DEFAULT 0,
  currency character varying NOT NULL DEFAULT 'SGD'::character varying,
  CONSTRAINT registrations_pkey PRIMARY KEY (id),
  CONSTRAINT registrations_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id),
  CONSTRAINT registrations_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id),
  CONSTRAINT registrations_ticket_tier_id_fkey FOREIGN KEY (ticket_tier_id) REFERENCES public.event_ticket_tiers(id)
);
CREATE TABLE public.event_reviews (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  event_id uuid NOT NULL,
  user_id uuid NOT NULL,
  rating integer CHECK (rating >= 1 AND rating <= 5),
  comment text,
  created_at timestamp without time zone DEFAULT now(),
  parent_id uuid,
  CONSTRAINT event_reviews_pkey PRIMARY KEY (id),
  CONSTRAINT reviews_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id),
  CONSTRAINT reviews_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id),
  CONSTRAINT event_reviews_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.event_reviews(id)
);
CREATE TABLE public.posts (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL,
  location_id uuid,
  event_id uuid,
  title character varying NOT NULL,
  content text NOT NULL,
  status character varying DEFAULT 'PUBLISHED'::character varying CHECK (status::text = ANY (ARRAY['DRAFT'::character varying::text, 'PUBLISHED'::character varying::text, 'ARCHIVED'::character varying::text])),
  created_at timestamp without time zone DEFAULT now(),
  updated_at timestamp without time zone DEFAULT now(),
  recommendation_score double precision NOT NULL DEFAULT 0,
  recommendation_score_updated_at timestamp without time zone,
  CONSTRAINT posts_pkey PRIMARY KEY (id),
  CONSTRAINT posts_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id),
  CONSTRAINT posts_location_id_fkey FOREIGN KEY (location_id) REFERENCES public.locations(id),
  CONSTRAINT posts_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id)
);
CREATE TABLE public.post_images (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  post_id uuid NOT NULL,
  image_url text NOT NULL,
  created_at timestamp without time zone DEFAULT now(),
  CONSTRAINT post_images_pkey PRIMARY KEY (id),
  CONSTRAINT post_images_post_id_fkey FOREIGN KEY (post_id) REFERENCES public.posts(id)
);
CREATE TABLE public.post_reviews (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  post_id uuid NOT NULL,
  user_id uuid NOT NULL,
  rating integer CHECK (rating >= 1 AND rating <= 5),
  comment text NOT NULL,
  created_at timestamp without time zone DEFAULT now(),
  parent_id uuid,
  CONSTRAINT post_reviews_pkey PRIMARY KEY (id),
  CONSTRAINT post_reviews_post_id_fkey FOREIGN KEY (post_id) REFERENCES public.posts(id),
  CONSTRAINT post_reviews_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id),
  CONSTRAINT post_reviews_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.post_reviews(id)
);
CREATE TABLE public.location_accessibility (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  location_id uuid NOT NULL UNIQUE,
  wheelchair_accessible boolean DEFAULT false,
  has_elevator boolean DEFAULT false,
  accessible_toilet boolean DEFAULT false,
  quiet_environment boolean DEFAULT false,
  step_free_access boolean DEFAULT false,
  notes text,
  created_at timestamp without time zone DEFAULT now(),
  updated_at timestamp without time zone DEFAULT now(),
  CONSTRAINT location_accessibility_pkey PRIMARY KEY (id),
  CONSTRAINT location_accessibility_location_id_fkey FOREIGN KEY (location_id) REFERENCES public.locations(id)
);
CREATE TABLE public.friend_requests (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  requester_id uuid NOT NULL,
  addressee_id uuid NOT NULL,
  status character varying NOT NULL DEFAULT 'PENDING'::character varying CHECK (status::text = ANY (ARRAY['PENDING'::character varying::text, 'ACCEPTED'::character varying::text, 'REJECTED'::character varying::text, 'CANCELLED'::character varying::text, 'BLOCKED'::character varying::text])),
  created_at timestamp without time zone DEFAULT now(),
  updated_at timestamp without time zone DEFAULT now(),
  CONSTRAINT friend_requests_pkey PRIMARY KEY (id),
  CONSTRAINT friend_requests_requester_id_fkey FOREIGN KEY (requester_id) REFERENCES public.users(id),
  CONSTRAINT friend_requests_addressee_id_fkey FOREIGN KEY (addressee_id) REFERENCES public.users(id)
);
CREATE TABLE public.chat_conversations (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  type character varying NOT NULL DEFAULT 'DIRECT'::character varying CHECK (type::text = ANY (ARRAY['DIRECT'::character varying::text, 'GROUP'::character varying::text])),
  direct_user_one_id uuid,
  direct_user_two_id uuid,
  created_at timestamp without time zone DEFAULT now(),
  updated_at timestamp without time zone DEFAULT now(),
  event_id uuid,
  CONSTRAINT chat_conversations_pkey PRIMARY KEY (id),
  CONSTRAINT chat_conversations_direct_user_one_id_fkey FOREIGN KEY (direct_user_one_id) REFERENCES public.users(id),
  CONSTRAINT chat_conversations_direct_user_two_id_fkey FOREIGN KEY (direct_user_two_id) REFERENCES public.users(id),
  CONSTRAINT chat_conversations_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id)
);
CREATE TABLE public.chat_participants (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  conversation_id uuid NOT NULL,
  user_id uuid NOT NULL,
  last_read_message_id uuid,
  joined_at timestamp without time zone DEFAULT now(),
  CONSTRAINT chat_participants_pkey PRIMARY KEY (id),
  CONSTRAINT chat_participants_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id),
  CONSTRAINT chat_participants_last_read_message_id_fkey FOREIGN KEY (last_read_message_id) REFERENCES public.chat_messages(id),
  CONSTRAINT chat_participants_conversation_id_fkey FOREIGN KEY (conversation_id) REFERENCES public.chat_conversations(id)
);
CREATE TABLE public.chat_messages (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  conversation_id uuid NOT NULL,
  sender_id uuid NOT NULL,
  content text NOT NULL CHECK (length(TRIM(BOTH FROM content)) > 0),
  message_type character varying NOT NULL DEFAULT 'TEXT'::character varying CHECK (message_type::text = ANY (ARRAY['TEXT'::character varying::text, 'IMAGE'::character varying::text, 'SYSTEM'::character varying::text])),
  created_at timestamp without time zone DEFAULT now(),
  edited_at timestamp without time zone,
  deleted_at timestamp without time zone,
  CONSTRAINT chat_messages_pkey PRIMARY KEY (id),
  CONSTRAINT chat_messages_conversation_id_fkey FOREIGN KEY (conversation_id) REFERENCES public.chat_conversations(id),
  CONSTRAINT chat_messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id)
);
CREATE TABLE public.user_accessibility_preferences (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL UNIQUE,
  wheelchair_accessible boolean NOT NULL DEFAULT false,
  elevator_needed boolean NOT NULL DEFAULT false,
  accessible_restroom boolean NOT NULL DEFAULT false,
  quiet_environment boolean NOT NULL DEFAULT false,
  created_at timestamp without time zone DEFAULT now(),
  updated_at timestamp without time zone DEFAULT now(),
  CONSTRAINT user_accessibility_preferences_pkey PRIMARY KEY (id),
  CONSTRAINT user_accessibility_preferences_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);
CREATE TABLE public.accessibility_features (
  key text NOT NULL,
  label text NOT NULL,
  created_at timestamp without time zone DEFAULT now(),
  CONSTRAINT accessibility_features_pkey PRIMARY KEY (key)
);
CREATE TABLE public.accessibility_preferences (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  user_id uuid,
  location_id uuid,
  feature_key text NOT NULL,
  notes text,
  created_at timestamp without time zone DEFAULT now(),
  updated_at timestamp without time zone DEFAULT now(),
  CONSTRAINT accessibility_preferences_pkey PRIMARY KEY (id),
  CONSTRAINT accessibility_preferences_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id),
  CONSTRAINT accessibility_preferences_location_id_fkey FOREIGN KEY (location_id) REFERENCES public.locations(id),
  CONSTRAINT accessibility_preferences_feature_key_fkey FOREIGN KEY (feature_key) REFERENCES public.accessibility_features(key)
);
CREATE TABLE public.notifications (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  recipient_id uuid NOT NULL,
  actor_id uuid,
  type character varying NOT NULL CHECK (type::text = ANY (ARRAY['FRIEND_REQUEST_RECEIVED'::text, 'FRIEND_REQUEST_ACCEPTED'::text, 'FRIEND_REQUEST_REJECTED'::text, 'CHAT_MESSAGE_RECEIVED'::text, 'EVENT_REGISTRATION_CREATED'::text, 'EVENT_REGISTRATION_STATUS_CHANGED'::text, 'POST_REVIEW_CREATED'::text, 'EVENT_REVIEW_CREATED'::text, 'EVENT_UPDATED'::text, 'EVENT_CANCELLED'::text, 'EVENT_STARTS_IN_1_DAY'::text, 'EVENT_STARTS_IN_2_HOURS'::text])),
  title character varying NOT NULL,
  body text,
  target_type character varying NOT NULL,
  target_id uuid NOT NULL,
  source_type character varying NOT NULL,
  source_id uuid NOT NULL,
  dedupe_key character varying NOT NULL,
  payload jsonb,
  read_at timestamp without time zone,
  archived_at timestamp without time zone,
  created_at timestamp without time zone DEFAULT now(),
  CONSTRAINT notifications_pkey PRIMARY KEY (id),
  CONSTRAINT notifications_recipient_id_fkey FOREIGN KEY (recipient_id) REFERENCES public.users(id),
  CONSTRAINT notifications_actor_id_fkey FOREIGN KEY (actor_id) REFERENCES public.users(id)
);
CREATE TABLE public.event_saves (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL,
  event_id uuid NOT NULL,
  created_at timestamp without time zone NOT NULL DEFAULT now(),
  CONSTRAINT event_saves_pkey PRIMARY KEY (id),
  CONSTRAINT event_saves_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id),
  CONSTRAINT event_saves_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id)
);
CREATE TABLE public.event_ticket_tiers (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  event_id uuid NOT NULL,
  name character varying NOT NULL CHECK (length(TRIM(BOTH FROM name)) > 0),
  type character varying NOT NULL DEFAULT 'STANDARD'::character varying,
  price numeric NOT NULL DEFAULT 0 CHECK (price >= 0::numeric),
  capacity integer NOT NULL DEFAULT 0 CHECK (capacity >= 0),
  sales_start_at timestamp without time zone,
  sales_end_at timestamp without time zone,
  active boolean NOT NULL DEFAULT true,
  sort_order integer NOT NULL DEFAULT 0,
  created_at timestamp without time zone DEFAULT now(),
  updated_at timestamp without time zone DEFAULT now(),
  CONSTRAINT event_ticket_tiers_pkey PRIMARY KEY (id),
  CONSTRAINT event_ticket_tiers_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.events(id)
);
