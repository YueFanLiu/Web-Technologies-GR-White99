-- Supabase Storage policies
-- Run this in Supabase SQL Editor: https://supabase.com/dashboard/project/mbtrnkskilrkzvqqqpbi/sql
--
-- Backend upload paths:
--   images/eventImages/{eventId}/...
--   images/locationImages/{locationId}/...
--   images/postImages/{postId}/...
--   images/userAvatar/{userId}/...

-- 1. Create the bucket (public = true means files are publicly accessible via URL)
INSERT INTO storage.buckets (id, name, public)
VALUES ('images', 'images', true)
ON CONFLICT (id) DO UPDATE SET public = true;

-- 2. Allow anyone to read images
DROP POLICY IF EXISTS "Public can read image objects" ON storage.objects;
CREATE POLICY "Public can read image objects"
ON storage.objects
FOR SELECT
TO public
USING (bucket_id = 'images');

-- 3. Allow insert from backend (backend handles its own auth via Controller layer)
DROP POLICY IF EXISTS "Authenticated users can upload app images" ON storage.objects;
CREATE POLICY "Anyone can upload app images"
ON storage.objects
FOR INSERT
TO public
WITH CHECK (
    bucket_id = 'images'
    AND (storage.foldername(name))[1] IN (
        'eventImages',
        'locationImages',
        'postImages',
        'userAvatar'
    )
);
