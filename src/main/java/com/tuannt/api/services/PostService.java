package com.tuannt.api.services;

import com.tuannt.api.constants.PostStatus;
import com.tuannt.api.dtos.PageResDto;
import com.tuannt.api.dtos.post.PostDto;
import com.tuannt.api.dtos.post.PostReqDto;
import com.tuannt.api.dtos.post.PostSummaryDto;
import com.tuannt.api.dtos.post.TagCountDto;

import java.util.List;

/**
 * Created by tuannt7 on 27/10/2026
 */
public interface PostService {
    /** Listed posts (published, due, not unlisted), newest first; tag and query may be null. */
    PageResDto<PostSummaryDto> listPublished(int page, int size, String tag, String query);

    /**
     * A published, due post by slug (unlisted ones included). MEMBERS posts come back without
     * their body unless {@code member}. A matching {@code previewKey} returns any post, drafts
     * and scheduled ones included. Throws NotFoundException for anything else.
     */
    PostDto getPublished(String slug, boolean member, String previewKey);

    /** Counts a read, at most once per visitor and post in a few hours. Unknown slugs are ignored. */
    void recordView(String slug, String clientIp);

    /** Up to {@code limit} listed posts sharing the most tags with this one, newest first on ties. */
    List<PostSummaryDto> related(String slug, int limit);

    /** Tags of listed posts, most used first. */
    List<TagCountDto> publishedTags();

    /** Drafts included, most recently updated first; status may be null for all. */
    PageResDto<PostSummaryDto> listAll(int page, int size, PostStatus status);

    PostDto get(Long id);

    PostDto create(PostReqDto req);

    PostDto update(Long id, PostReqDto req);

    void delete(Long id);

    /** Issues a new preview secret, replacing (and so revoking) any previous link. */
    PostDto createPreviewKey(Long id);

    void revokePreviewKey(Long id);
}
