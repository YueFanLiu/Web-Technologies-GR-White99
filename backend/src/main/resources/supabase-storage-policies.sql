-- Supabase Storage policies required by backend multipart image uploads.
--
-- Run this in the Supabase SQL Editor for the project used by application.yaml.
-- The backend uploads files to:
--   images/eventImages/{eventId}/...
--   images/locationImages/{locationId}/...
--   images/postImages/{postId}/...
--   images/userAvatar/{userId}/...

INSERT INTO storage.buckets (id, name, public)
VALUES ('images', 'images', true)
ON CONFLICT (id) DO UPDATE SET public = true;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_policies
        WHERE schemaname = 'storage'
          AND tablename = 'objects'
          AND policyname = 'Public can read image objects'
    ) THEN
        CREATE POLICY "Public can read image objects"
        ON storage.objects
        FOR SELECT
        TO public
        USING (bucket_id = 'images');
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_policies
        WHERE schemaname = 'storage'
          AND tablename = 'objects'
          AND policyname = 'Authenticated users can upload app images'
    ) THEN
        CREATE POLICY "Authenticated users can upload app images"
        ON storage.objects
        FOR INSERT
        TO authenticated
        WITH CHECK (
            bucket_id = 'images'
            AND (storage.foldername(name))[1] IN (
                'eventImages',
                'locationImages',
                'postImages',
                'userAvatar'
            )
        );
    END IF;
END $$;
