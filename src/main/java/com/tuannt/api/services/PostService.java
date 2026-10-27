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
    /** Published posts only, newest first; tag may be null. */
    PageResDto<PostSummaryDto> listPublished(int page, int size, String tag);

    /** Throws NotFoundException for a missing or unpublished slug alike. */
    PostDto getPublished(String slug);

    /** Tags of published posts, most used first. */
    List<TagCountDto> publishedTags();

    /** Drafts included, most recently updated first; status may be null for all. */
    PageResDto<PostSummaryDto> listAll(int page, int size, PostStatus status);

    PostDto get(Long id);

    PostDto create(PostReqDto req);

    PostDto update(Long id, PostReqDto req);

    void delete(Long id);
}
