package com.tuannt.api.dtos.project;

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
public class ProjectReqDto extends BaseDto {
    /** Optional: derived from the name on create, kept unchanged on update when left empty. */
    @Size(max = ValidationPatterns.SLUG_MAX_LENGTH, message = "slug must be at most 120 characters")
    @Pattern(regexp = ValidationPatterns.OPTIONAL_SLUG,
            message = "slug must be lowercase letters, digits and single dashes, like my-project")
    private String slug;

    @NotBlank(message = "name is required")
    @Size(max = 200, message = "name must be at most 200 characters")
    private String name;

    @NotBlank(message = "summary is required")
    @Size(max = 300, message = "summary must be at most 300 characters")
    private String summary;

    @Size(max = 100_000, message = "description must be at most 100000 characters")
    private String description;

    @NotNull(message = "tech must be an array")
    @Size(max = 20, message = "at most 20 tech entries")
    private List<@NotBlank(message = "tech must not contain blank values")
    @Size(max = 50, message = "each tech entry must be at most 50 characters") String> tech = new ArrayList<>();

    @Size(max = 500, message = "repoUrl must be at most 500 characters")
    @Pattern(regexp = ValidationPatterns.OPTIONAL_LINK, message = "repoUrl must be an http(s) URL or a path starting with /")
    private String repoUrl;

    @Size(max = 500, message = "demoUrl must be at most 500 characters")
    @Pattern(regexp = ValidationPatterns.OPTIONAL_LINK, message = "demoUrl must be an http(s) URL or a path starting with /")
    private String demoUrl;

    @Size(max = 500, message = "imageUrl must be at most 500 characters")
    @Pattern(regexp = ValidationPatterns.OPTIONAL_LINK, message = "imageUrl must be an http(s) URL or a path starting with /")
    private String imageUrl;

    private boolean featured;

    /** Ascending within the featured and non-featured groups. */
    private int sortOrder;

    private boolean visible = true;

    @Override
    public String toString() {
        return JsonUtil.toJsonAbbreviated(this, "description");
    }
}
