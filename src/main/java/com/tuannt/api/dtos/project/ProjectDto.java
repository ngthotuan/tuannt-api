package com.tuannt.api.dtos.project;

import com.tuannt.api.dtos.BaseDto;
import com.tuannt.api.utils.JsonUtil;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Created by tuannt7 on 27/10/2026
 */

@Getter
@Builder
public class ProjectDto extends BaseDto {
    private Long id;
    private String slug;
    private String name;
    private String summary;
    /** Markdown, may be null. */
    private String description;
    private List<String> tech;
    private String repoUrl;
    private String demoUrl;
    private String imageUrl;
    private boolean featured;
    private int sortOrder;
    private boolean visible;
    /** Epoch millis. */
    private Long createdAt;
    private Long updatedAt;

    @Override
    public String toString() {
        return JsonUtil.toJsonAbbreviated(this, "description");
    }
}
