package com.semih.siteapi.tag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findBySlugIn(Collection<String> slugs);

    /**
     * Tags used by at least one published post, most used first. Counted in the database with a
     * single grouped query (a DTO projection), instead of loading every post to count in Java.
     */
    @Query("""
            select new com.semih.siteapi.tag.TagUsage(t.name, t.slug, count(p))
            from Post p join p.tags t
            where p.status = com.semih.siteapi.post.PostStatus.PUBLISHED
            group by t.id, t.name, t.slug
            order by count(p) desc, t.slug""")
    List<TagUsage> findPublishedUsage();
}
