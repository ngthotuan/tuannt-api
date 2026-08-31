package com.tuannt.api.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

/**
 * Created by tuannt7 on 29/02/2024
 */
@Getter
@AllArgsConstructor
public enum TransactionStatus {
    PROCESSING(2),
    SUCCESSFUL(1),
    PENDING(3),
    FAILED(-999),
    INVALID_DATA(-1),
    LOCK_FAIL(-2),
    BALANCE_NOT_ENOUGH(-3),
    WALLET_NOT_FOUND(-4),
    DEDUCT_ERROR(-5),
    ;
    private final int status;

    public static TransactionStatus of(int status) {
        return Stream.of(TransactionStatus.values())
                .filter(p -> p.status == status)
                .findFirst()
                .orElse(FAILED);
    }
}
