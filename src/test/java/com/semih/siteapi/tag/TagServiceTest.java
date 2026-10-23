package com.semih.siteapi.tag;

import com.semih.siteapi.common.exception.InvalidTagException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    private TagService tagService;

    @BeforeEach
    void setUp() {
        tagService = new TagService(tagRepository, new TagMapper());
    }

    @Test
    void existingTagsAreReusedAndOnlyNewOnesAreCreated() {
        Tag existing = new Tag("Spring Boot", "spring-boot");
        when(tagRepository.findBySlugIn(any())).thenReturn(List.of(existing));
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Set<Tag> tags = tagService.resolve(List.of("spring boot", " Veritabanı "));

        assertThat(tags).extracting(Tag::getSlug).containsExactlyInAnyOrder("spring-boot", "veritabani");
        // The existing tag is reused as it is, with its original spelling
        assertThat(tags).anySatisfy(tag -> assertThat(tag).isSameAs(existing));
        verify(tagRepository, times(1)).save(any(Tag.class));
    }

    @Test
    void namesThatMakeTheSameSlugBecomeOneTagLookedUpInOneQuery() {
        when(tagRepository.findBySlugIn(any())).thenReturn(List.of());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Set<Tag> tags = tagService.resolve(List.of("Java", "JAVA", "java "));

        assertThat(tags).singleElement().satisfies(tag -> {
            assertThat(tag.getSlug()).isEqualTo("java");
            assertThat(tag.getName()).isEqualTo("Java");
        });
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> slugs = ArgumentCaptor.forClass(Collection.class);
        verify(tagRepository).findBySlugIn(slugs.capture());
        assertThat(slugs.getValue()).containsExactly("java");
    }

    @Test
    void noTagsMeansNoQueries() {
        assertThat(tagService.resolve(null)).isEmpty();
        assertThat(tagService.resolve(List.of())).isEmpty();

        verify(tagRepository, never()).findBySlugIn(any());
    }

    @Test
    void aNameWithoutLatinLettersOrDigitsIsRejected() {
        assertThatThrownBy(() -> tagService.resolve(List.of("Java", "日本語")))
                .isInstanceOf(InvalidTagException.class)
                .hasMessageContaining("日本語");

        verify(tagRepository, never()).save(any());
    }
}
