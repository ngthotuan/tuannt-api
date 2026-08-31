package com.tuannt.api.dtos.contact;

import com.tuannt.api.dtos.BaseDto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by tuannt7 on 01/09/2026
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageReqDto extends BaseDto {
    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must be at most 100 characters")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "email is invalid")
    @Size(max = 255, message = "email must be at most 255 characters")
    private String email;

    @NotBlank(message = "subject is required")
    @Size(max = 200, message = "subject must be at most 200 characters")
    private String subject;

    @NotBlank(message = "message is required")
    @Size(max = 4000, message = "message must be at most 4000 characters")
    private String message;
}
