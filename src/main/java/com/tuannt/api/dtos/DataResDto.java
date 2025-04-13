package com.tuannt.api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatusCode;

/**
 * Created by tuannt7 on 15/04/2024
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataResDto<V> extends BaseDto {
    private HttpStatusCode status;
    private V data;
}
