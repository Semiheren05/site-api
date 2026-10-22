package com.semih.siteapi.post;

import com.semih.siteapi.common.exception.InvalidSlugException;
import com.semih.siteapi.common.exception.ResourceNotFoundException;
import com.semih.siteapi.common.exception.SlugAlreadyUsedException;
import com.semih.siteapi.post.dto.PostRequest;
import com.semih.siteapi.post.dto.PostResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-22T09:00:00Z");

    @Mock
    private PostRepository postRepository;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createBuildsSlugFromTitleAndStartsAsDraft() {
        when(postRepository.existsBySlug("spring-boot-ile-ilk-api")).thenReturn(false);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostResponse response = postService.create(
                new PostRequest("  Spring Boot ile İlk API ", null, "  ", "İçerik"));

        ArgumentCaptor<Post> saved = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(saved.capture());
        assertThat(saved.getValue().getSlug()).isEqualTo("spring-boot-ile-ilk-api");
        assertThat(saved.getValue().getTitle()).isEqualTo("Spring Boot ile İlk API");
        assertThat(saved.getValue().getSummary()).isNull();
        assertThat(response.status()).isEqualTo(PostStatus.DRAFT);
        assertThat(response.publishedAt()).isNull();
    }

    @Test
    void createUsesTheGivenSlug() {
        when(postRepository.existsBySlug("ozel-adres")).thenReturn(false);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostResponse response = postService.create(new PostRequest("Başlık", "ozel-adres", null, "İçerik"));

        assertThat(response.slug()).isEqualTo("ozel-adres");
    }

    @Test
    void createRejectsATakenSlug() {
        when(postRepository.existsBySlug("merhaba")).thenReturn(true);

        assertThatThrownBy(() -> postService.create(new PostRequest("Merhaba", null, null, "İçerik")))
                .isInstanceOf(SlugAlreadyUsedException.class);

        verify(postRepository, never()).save(any());
    }

    @Test
    void createRejectsATitleWithoutLettersWhenNoSlugIsGiven() {
        assertThatThrownBy(() -> postService.create(new PostRequest("???", null, null, "İçerik")))
                .isInstanceOf(InvalidSlugException.class);
    }

    @Test
    void updateRejectsASlugUsedByAnotherPost() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(new Post("eski", "Eski", null, "İçerik")));
        when(postRepository.existsBySlugAndIdNot("dolu", 1L)).thenReturn(true);

        assertThatThrownBy(() -> postService.update(1L, new PostRequest("Yeni", "dolu", null, "İçerik")))
                .isInstanceOf(SlugAlreadyUsedException.class);
    }

    @Test
    void publishSetsThePublishDateFromTheClockOnlyOnce() {
        Post post = new Post("yazi", "Yazı", null, "İçerik");
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        postService.publish(1L);
        postService.unpublish(1L);
        post.publish(NOW.plusSeconds(3600));

        assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(post.getPublishedAt()).isEqualTo(NOW);
    }

    @Test
    void unknownPostIdGives404() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.publish(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void draftsAreNotVisibleOnThePublicApi() {
        when(postRepository.findBySlugAndStatus("taslak", PostStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPublished("taslak")).isInstanceOf(ResourceNotFoundException.class);
    }
}
