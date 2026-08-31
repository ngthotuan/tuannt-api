package com.tuannt.api.dtos;


import com.tuannt.api.utils.CommonUtil;

import java.io.Serializable;

/**
 * Created by tuannt7 on 12/04/2024
 */

public abstract class BaseDto implements Serializable {
    @Override
    public String toString() {
        return CommonUtil.objectToJsonString(this);
    }
}
