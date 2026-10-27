package com.tuannt.api.dtos.post;

import com.tuannt.api.constants.PostStatus;
import com.tuannt.api.constants.ValidationPatterns;
import com.tuannt.api.dtos.BaseDto;
import com.tuannt.api.utils.JsonUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * Body of both create and update; an update replaces every field.
 */

@Getter
@Setter
public class PostReqDto extends BaseDto {
    /** Optional: derived from the title on create, kept unchanged on update when left empty. */
    @Size(max = ValidationPatterns.SLUG_MAX_LENGTH, message = "slug must be at most 120 characters")
    @Pattern(regexp = ValidationPatterns.OPTIONAL_SLUG,
            message = "slug must be lowercase letters, digits and single dashes, like my-first-post")
    private String slug;

    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title must be at most 200 characters")
    private String title;

    @Size(max = 500, message = "excerpt must be at most 500 characters")
    private String excerpt;

    @NotNull(message = "content is required")
    @Size(max = 500_000, message = "content must be at most 500000 characters")
    private String content;

    @Size(max = 500, message = "coverUrl must be at most 500 characters")
    @Pattern(regexp = ValidationPatterns.OPTIONAL_LINK, message = "coverUrl must be an http(s) URL or a path starting with /")
    private String coverUrl;

    /** Normalised to lowercase kebab-case by the server ("Spring Boot" becomes "spring-boot"). */
    @NotNull(message = "tags must be an array")
    @Size(max = 10, message = "at most 10 tags")
    private List<@NotBlank(message = "tags must not contain blank values")
    @Size(max = 40, message = "each tag must be at most 40 characters") String> tags = new ArrayList<>();

    @NotNull(message = "status must be DRAFT or PUBLISHED")
    private PostStatus status = PostStatus.DRAFT;

    // LoggingAspect logs request bodies through toString; a whole Markdown document would flood it.
    @Override
    public String toString() {
        return JsonUtil.toJsonAbbreviated(this, "content");
    }
}
