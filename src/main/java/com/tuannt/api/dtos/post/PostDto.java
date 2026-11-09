package com.tuannt.api.dtos.post;

import com.tuannt.api.constants.PostStatus;
import com.tuannt.api.constants.PostVisibility;
import com.tuannt.api.dtos.BaseDto;
import com.tuannt.api.utils.JsonUtil;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * A full post: the summary fields plus the Markdown body.
 */

@Getter
@Builder
public class PostDto extends BaseDto {
    private Long id;
    private String slug;
    private String title;
    private String excerpt;
    /** Markdown. */
    private String content;
    private String coverUrl;
    private List<String> tags;
    private PostStatus status;
    private PostVisibility visibility;
    private long viewCount;
    /** Epoch millis; null until the post is first published. */
    private Long publishedAt;
    private Long createdAt;
    private Long updatedAt;
    /** True when the body was withheld: a MEMBERS post read without logging in. */
    private boolean locked;
    /** Admin responses only. */
    private String previewKey;

    @Override
    public String toString() {
        return JsonUtil.toJsonAbbreviated(this, "content");
    }
}
