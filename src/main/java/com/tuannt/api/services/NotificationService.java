package com.tuannt.api.services;

/**
 * Created by tuannt7 on 02/05/2023
 */
public interface NotificationService {
    boolean sentMessage(String to, String title, String message);

    boolean sentMessage(String title, String message);
}
