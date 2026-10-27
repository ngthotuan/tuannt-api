package com.tuannt.api.dtos.contact;

import com.tuannt.api.dtos.BaseDto;
import lombok.Builder;
import lombok.Getter;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * A message as the admin inbox shows it.
 */

@Getter
@Builder
public class ContactMessageDto extends BaseDto {
    private Long id;
    private String name;
    private String email;
    private String subject;
    private String message;
    /** Sender IP as seen by the API, useful for spotting spam. */
    private String ip;
    /** Epoch millis. */
    private Long createdAt;
    /** Epoch millis; null while unread. */
    private Long readAt;
}
