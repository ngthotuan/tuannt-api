package com.tuannt.api.constants;

/**
 * Created by tuannt7 on 27/10/2026
 */
public final class ValidationPatterns {
    /** Lowercase kebab-case, or empty to have the server derive one from the title/name. */
    public static final String OPTIONAL_SLUG = "|[a-z0-9]+(-[a-z0-9]+)*";
    public static final int SLUG_MAX_LENGTH = 120;
    /**
     * Absolute http(s) URL or a site-relative path like /static/img/x.png, or empty for none.
     * Anything else, such as javascript:, would become a live link in the static site's HTML.
     */
    public static final String OPTIONAL_LINK = "|(https?://|/)\\S*";

    private ValidationPatterns() {
    }
}
