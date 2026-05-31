alter table public.post_reviews
    add column if not exists parent_id uuid;

alter table public.post_reviews
    drop constraint if exists post_reviews_parent_id_fkey;

alter table public.post_reviews
    add constraint post_reviews_parent_id_fkey
        foreign key (parent_id)
        references public.post_reviews(id)
        on delete cascade;

create index if not exists idx_post_reviews_parent_id
    on public.post_reviews using btree (parent_id);
