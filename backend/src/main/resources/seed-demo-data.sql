-- Demo data for recommendation testing.
-- Counts: 10 locations, 25 events, 60 posts.
--
-- This script follows the current public schema only.
-- It does not create users because public.users.id references auth.users(id).
-- Before running it, make sure public.users contains at least one user.
-- Preferably create at least one ORGANIZER user and one PARENT user.

DO $$
DECLARE
    demo_organizer_id uuid;
    demo_author_id uuid;
    location_ids uuid[] := ARRAY[
        '11111111-1111-1111-1111-111111111101',
        '11111111-1111-1111-1111-111111111102',
        '11111111-1111-1111-1111-111111111103',
        '11111111-1111-1111-1111-111111111104',
        '11111111-1111-1111-1111-111111111105',
        '11111111-1111-1111-1111-111111111106',
        '11111111-1111-1111-1111-111111111107',
        '11111111-1111-1111-1111-111111111108',
        '11111111-1111-1111-1111-111111111109',
        '11111111-1111-1111-1111-111111111110'
    ]::uuid[];
    event_ids uuid[] := ARRAY[
        '22222222-2222-2222-2222-222222222201',
        '22222222-2222-2222-2222-222222222202',
        '22222222-2222-2222-2222-222222222203',
        '22222222-2222-2222-2222-222222222204',
        '22222222-2222-2222-2222-222222222205',
        '22222222-2222-2222-2222-222222222206',
        '22222222-2222-2222-2222-222222222207',
        '22222222-2222-2222-2222-222222222208',
        '22222222-2222-2222-2222-222222222209',
        '22222222-2222-2222-2222-222222222210',
        '22222222-2222-2222-2222-222222222211',
        '22222222-2222-2222-2222-222222222212',
        '22222222-2222-2222-2222-222222222213',
        '22222222-2222-2222-2222-222222222214',
        '22222222-2222-2222-2222-222222222215',
        '22222222-2222-2222-2222-222222222216',
        '22222222-2222-2222-2222-222222222217',
        '22222222-2222-2222-2222-222222222218',
        '22222222-2222-2222-2222-222222222219',
        '22222222-2222-2222-2222-222222222220',
        '22222222-2222-2222-2222-222222222221',
        '22222222-2222-2222-2222-222222222222',
        '22222222-2222-2222-2222-222222222223',
        '22222222-2222-2222-2222-222222222224',
        '22222222-2222-2222-2222-222222222225'
    ]::uuid[];
BEGIN
    SELECT id
    INTO demo_organizer_id
    FROM public.users
    WHERE role = 'ORGANIZER'
    ORDER BY created_at ASC
    LIMIT 1;

    IF demo_organizer_id IS NULL THEN
        SELECT id
        INTO demo_organizer_id
        FROM public.users
        ORDER BY created_at ASC
        LIMIT 1;
    END IF;

    SELECT id
    INTO demo_author_id
    FROM public.users
    WHERE role = 'PARENT'
    ORDER BY created_at ASC
    LIMIT 1;

    IF demo_author_id IS NULL THEN
        SELECT id
        INTO demo_author_id
        FROM public.users
        ORDER BY created_at ASC
        LIMIT 1;
    END IF;

    IF demo_organizer_id IS NULL OR demo_author_id IS NULL THEN
        RAISE EXCEPTION 'Seed aborted: public.users must contain at least one user.';
    END IF;

    INSERT INTO public.locations (
        id, name, description, address, city, country, latitude, longitude
    )
    VALUES
        (location_ids[1], 'Inclusive Arts Center', 'Accessible arts center with workshops, music rooms, and family events.', '12 Rue des Arts', 'Paris', 'France', 48.8566, 2.3522),
        (location_ids[2], 'Green Family Park', 'Large outdoor park with playgrounds, picnic areas, and weekend activities.', '45 Avenue Verte', 'Paris', 'France', 48.8738, 2.2950),
        (location_ids[3], 'Community Science Hub', 'Hands-on science space for children, parents, and school groups.', '8 Innovation Street', 'Lyon', 'France', 45.7640, 4.8357),
        (location_ids[4], 'Quiet Learning Library', 'Calm library with reading rooms, study corners, and sensory-friendly sessions.', '21 Library Road', 'Lyon', 'France', 45.7500, 4.8500),
        (location_ids[5], 'Riverside Sports Hall', 'Indoor sports hall for family games, movement classes, and local tournaments.', '3 Riverside Lane', 'Bordeaux', 'France', 44.8378, -0.5792),
        (location_ids[6], 'Creative Kids Studio', 'Small creative studio focused on drawing, crafts, and weekend parent-child classes.', '77 Studio Passage', 'Lille', 'France', 50.6292, 3.0573),
        (location_ids[7], 'Digital Discovery Lab', 'Technology lab with coding workshops, robotics tables, and guided demos.', '14 Tech Square', 'Toulouse', 'France', 43.6047, 1.4442),
        (location_ids[8], 'Family Wellness Room', 'Wellness room for parent talks, calm activities, and small group meetings.', '9 Health Avenue', 'Nice', 'France', 43.7102, 7.2620),
        (location_ids[9], 'Open Air Theatre', 'Outdoor theatre for seasonal shows, music evenings, and community performances.', '5 Stage Garden', 'Marseille', 'France', 43.2965, 5.3698),
        (location_ids[10], 'Neighborhood Play Cafe', 'Cafe with supervised play corner, reading area, and parent meetups.', '31 Family Street', 'Nantes', 'France', 47.2184, -1.5536)
    ON CONFLICT (id) DO UPDATE SET
        name = EXCLUDED.name,
        description = EXCLUDED.description,
        address = EXCLUDED.address,
        city = EXCLUDED.city,
        country = EXCLUDED.country,
        latitude = EXCLUDED.latitude,
        longitude = EXCLUDED.longitude;

    INSERT INTO public.location_accessibility (
        location_id,
        wheelchair_accessible,
        has_elevator,
        accessible_toilet,
        quiet_environment,
        step_free_access,
        notes
    )
    VALUES
        (location_ids[1], true, true, true, false, true, 'Main entrance is step-free and staff can assist families.'),
        (location_ids[2], true, false, true, false, true, 'Outdoor paths are mostly flat, but some garden areas are uneven.'),
        (location_ids[3], true, true, true, false, true, 'Elevator available near the main reception.'),
        (location_ids[4], true, true, true, true, true, 'Quiet rooms are available during sensory-friendly hours.'),
        (location_ids[5], true, false, true, false, true, 'Sports floor is accessible from the side entrance.'),
        (location_ids[6], false, false, false, true, false, 'Small studio on the first floor without elevator.'),
        (location_ids[7], true, true, true, false, true, 'Robotics room has wide table spacing.'),
        (location_ids[8], true, true, true, true, true, 'Designed for calm small-group activities.'),
        (location_ids[9], false, false, false, false, false, 'Outdoor seating includes stairs and uneven paths.'),
        (location_ids[10], true, false, true, true, true, 'Step-free entrance and quiet morning hours.')
    ON CONFLICT (location_id) DO UPDATE SET
        wheelchair_accessible = EXCLUDED.wheelchair_accessible,
        has_elevator = EXCLUDED.has_elevator,
        accessible_toilet = EXCLUDED.accessible_toilet,
        quiet_environment = EXCLUDED.quiet_environment,
        step_free_access = EXCLUDED.step_free_access,
        notes = EXCLUDED.notes;

    INSERT INTO public.location_images (location_id, image_url)
    SELECT v.location_id, v.image_url
    FROM (
        VALUES
            (location_ids[1], 'https://placehold.co/800x600/png?text=Inclusive+Arts+Center'),
            (location_ids[1], 'https://placehold.co/800x600/png?text=Arts+Workshop'),
            (location_ids[2], 'https://placehold.co/800x600/png?text=Green+Family+Park'),
            (location_ids[3], 'https://placehold.co/800x600/png?text=Science+Hub'),
            (location_ids[4], 'https://placehold.co/800x600/png?text=Quiet+Learning+Library'),
            (location_ids[4], 'https://placehold.co/800x600/png?text=Reading+Room'),
            (location_ids[5], 'https://placehold.co/800x600/png?text=Sports+Hall'),
            (location_ids[7], 'https://placehold.co/800x600/png?text=Digital+Discovery+Lab'),
            (location_ids[8], 'https://placehold.co/800x600/png?text=Family+Wellness+Room'),
            (location_ids[10], 'https://placehold.co/800x600/png?text=Play+Cafe')
    ) AS v(location_id, image_url)
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.location_images li
        WHERE li.image_url = v.image_url
    );

    INSERT INTO public.events (
        id,
        organizer_id,
        location_id,
        title,
        description,
        category,
        start_time,
        end_time,
        capacity,
        price,
        is_virtual,
        status
    )
    SELECT
        event_ids[n],
        demo_organizer_id,
        location_ids[((n - 1) % array_length(location_ids, 1)) + 1],
        event_title,
        event_description,
        event_category,
        event_start,
        event_start + interval '2 hours',
        event_capacity,
        event_price,
        event_virtual,
        'PUBLISHED'
    FROM (
        VALUES
            (1, 'Family Painting Morning', 'A guided painting session for parents and children.', 'ART', timestamp '2026-05-12 10:00:00', 24, 8.00, false),
            (2, 'Park Discovery Walk', 'Outdoor discovery walk with simple nature games.', 'OUTDOOR', timestamp '2026-05-13 14:00:00', 35, 0.00, false),
            (3, 'Robotics for Beginners', 'Introductory robotics workshop using child-friendly kits.', 'TECH', timestamp '2026-05-15 16:00:00', 18, 12.00, false),
            (4, 'Quiet Reading Circle', 'Small reading group in a calm library room.', 'READING', timestamp '2026-05-16 11:00:00', 12, 0.00, false),
            (5, 'Indoor Family Games', 'Cooperative games and light sports for mixed ages.', 'SPORT', timestamp '2026-05-17 15:00:00', 30, 5.00, false),
            (6, 'Weekend Craft Lab', 'Hands-on craft session with paper, clay, and recycled materials.', 'ART', timestamp '2026-05-19 10:00:00', 16, 10.00, false),
            (7, 'Coding With Blocks', 'Visual coding activities for young beginners.', 'TECH', timestamp '2026-05-20 17:00:00', 20, 9.00, false),
            (8, 'Parent Wellness Talk', 'Short talk and discussion about family routines and wellbeing.', 'WELLNESS', timestamp '2026-05-21 18:00:00', 22, 0.00, false),
            (9, 'Outdoor Music Evening', 'Community music evening in an open-air theatre.', 'MUSIC', timestamp '2026-05-23 19:00:00', 80, 6.00, false),
            (10, 'Play Cafe Meetup', 'Informal meetup for parents while children use the play corner.', 'SOCIAL', timestamp '2026-05-24 09:30:00', 20, 4.00, false),
            (11, 'Accessible Dance Class', 'Movement class designed for mixed mobility levels.', 'SPORT', timestamp '2026-05-26 15:00:00', 18, 7.00, false),
            (12, 'Garden Picnic Club', 'Family picnic meetup with simple outdoor activities.', 'OUTDOOR', timestamp '2026-05-27 12:00:00', 45, 0.00, false),
            (13, 'Science Story Time', 'Stories connected to simple experiments for children.', 'SCIENCE', timestamp '2026-05-29 10:30:00', 25, 3.00, false),
            (14, 'Sensory Friendly Library Hour', 'Quiet library hour with low noise and small groups.', 'READING', timestamp '2026-05-30 10:00:00', 10, 0.00, false),
            (15, 'Mini Basketball Afternoon', 'Beginner basketball games for children and parents.', 'SPORT', timestamp '2026-06-02 16:00:00', 28, 6.00, false),
            (16, 'Clay Animals Workshop', 'Creative clay workshop for family pairs.', 'ART', timestamp '2026-06-03 14:00:00', 14, 11.00, false),
            (17, 'Robot Challenge Day', 'Small robotics challenges with guided support.', 'TECH', timestamp '2026-06-05 15:00:00', 18, 14.00, false),
            (18, 'Mindful Family Session', 'Breathing and relaxation exercises for families.', 'WELLNESS', timestamp '2026-06-06 11:00:00', 16, 5.00, false),
            (19, 'Children Theatre Night', 'Short outdoor theatre show for families.', 'THEATRE', timestamp '2026-06-08 18:30:00', 90, 8.00, false),
            (20, 'Story and Snack Morning', 'Small social morning with reading and snacks.', 'SOCIAL', timestamp '2026-06-09 10:00:00', 18, 4.00, false),
            (21, 'Online Parenting Q and A', 'Virtual session for parenting questions and local resources.', 'WELLNESS', timestamp '2026-06-11 20:00:00', 100, 0.00, true),
            (22, 'Past Spring Art Fair', 'Past art fair kept for history and ranking contrast.', 'ART', timestamp '2026-04-05 11:00:00', 60, 0.00, false),
            (23, 'Past Park Cleanup', 'Past community cleanup event.', 'OUTDOOR', timestamp '2026-04-12 09:00:00', 40, 0.00, false),
            (24, 'Past Coding Demo', 'Past technology demonstration session.', 'TECH', timestamp '2026-04-18 15:00:00', 30, 0.00, false),
            (25, 'Future Family Festival', 'Large family festival with activities across multiple zones.', 'FESTIVAL', timestamp '2026-07-04 13:00:00', 150, 15.00, false)
    ) AS v(n, event_title, event_description, event_category, event_start, event_capacity, event_price, event_virtual)
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.events e
        WHERE e.id = event_ids[n]
    );

    INSERT INTO public.event_images (event_id, image_url)
    SELECT v.event_id, v.image_url
    FROM (
        VALUES
            (event_ids[1], 'https://placehold.co/800x600/png?text=Family+Painting+Morning'),
            (event_ids[3], 'https://placehold.co/800x600/png?text=Robotics+For+Beginners'),
            (event_ids[7], 'https://placehold.co/800x600/png?text=Coding+With+Blocks'),
            (event_ids[9], 'https://placehold.co/800x600/png?text=Outdoor+Music+Evening'),
            (event_ids[14], 'https://placehold.co/800x600/png?text=Sensory+Friendly+Library+Hour'),
            (event_ids[19], 'https://placehold.co/800x600/png?text=Children+Theatre+Night'),
            (event_ids[25], 'https://placehold.co/800x600/png?text=Future+Family+Festival')
    ) AS v(event_id, image_url)
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.event_images ei
        WHERE ei.image_url = v.image_url
    );

    INSERT INTO public.posts (
        user_id,
        location_id,
        event_id,
        title,
        content,
        status
    )
    SELECT
        demo_author_id,
        location_ids[((n - 1) % array_length(location_ids, 1)) + 1],
        CASE
            WHEN n % 3 = 0 THEN event_ids[((n - 1) % array_length(event_ids, 1)) + 1]
            ELSE NULL
        END,
        'Demo Post ' || lpad(n::text, 2, '0') || ' - ' || post_topic,
        post_content,
        CASE
            WHEN n % 17 = 0 THEN 'DRAFT'
            WHEN n % 23 = 0 THEN 'ARCHIVED'
            ELSE 'PUBLISHED'
        END
    FROM (
        SELECT
            n,
            (ARRAY[
                'family accessibility notes',
                'weekend activity idea',
                'quiet space feedback',
                'playground experience',
                'event planning tip',
                'transport and arrival',
                'parent meetup recap',
                'children workshop review',
                'sensory friendly visit',
                'local community suggestion'
            ])[((n - 1) % 10) + 1] AS post_topic,
            (ARRAY[
                'The visit was easy to organize and the location felt useful for families.',
                'We found the activity suitable for children and parents who need a simple weekend plan.',
                'The quiet areas helped a lot, especially before and after the main session.',
                'The place was clear to navigate and the staff gave practical information.',
                'This could be a good option for future family events with mixed age groups.',
                'Transport was manageable and the address information was accurate.',
                'The meetup created a relaxed space for parents to exchange practical tips.',
                'The workshop format was short enough for children to stay focused.',
                'The accessibility details made it easier to decide before going.',
                'The community would benefit from more sessions like this during weekends.'
            ])[((n - 1) % 10) + 1] AS post_content
        FROM generate_series(1, 60) AS gs(n)
    ) AS generated_posts
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.posts p
        WHERE p.user_id = demo_author_id
          AND p.title = 'Demo Post ' || lpad(n::text, 2, '0') || ' - ' || post_topic
    );

    INSERT INTO public.post_images (post_id, image_url)
    SELECT p.id, 'https://placehold.co/800x600/png?text=' || replace(p.title, ' ', '+')
    FROM public.posts p
    WHERE p.user_id = demo_author_id
      AND p.title LIKE 'Demo Post %'
      AND (
          p.title LIKE 'Demo Post 01%'
          OR p.title LIKE 'Demo Post 05%'
          OR p.title LIKE 'Demo Post 11%'
          OR p.title LIKE 'Demo Post 18%'
          OR p.title LIKE 'Demo Post 24%'
          OR p.title LIKE 'Demo Post 31%'
          OR p.title LIKE 'Demo Post 37%'
          OR p.title LIKE 'Demo Post 44%'
          OR p.title LIKE 'Demo Post 52%'
          OR p.title LIKE 'Demo Post 59%'
      )
      AND NOT EXISTS (
          SELECT 1
          FROM public.post_images pi
          WHERE pi.post_id = p.id
      );
END $$;
