package com.tuannt.api.dtos.post;

import com.tuannt.api.constants.PostStatus;
import com.tuannt.api.dtos.BaseDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * A post in list responses: everything except the Markdown body.
 */

@Getter
@Builder
public class PostSummaryDto extends BaseDto {
    private Long id;
    private String slug;
    private String title;
    private String excerpt;
    private String coverUrl;
    private List<String> tags;
    private PostStatus status;
    /** Epoch millis; null until the post is first published. */
    private Long publishedAt;
    private Long createdAt;
    private Long updatedAt;
}
