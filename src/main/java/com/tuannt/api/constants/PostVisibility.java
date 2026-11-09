package com.tuannt.api.constants;

/**
 * Created by tuannt7 on 09/11/2026
 * <p>
 * Who can find and read a published post. Drafts and scheduled posts are hidden regardless.
 */
public enum PostVisibility {
    /** Listed and readable by anyone. */
    PUBLIC,
    /** Readable by anyone with the link, never listed (lists, tags, RSS, sitemap, related). */
    UNLISTED,
    /** Listed, but the body is only returned to logged-in users. */
    MEMBERS
}
