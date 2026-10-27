package com.tuannt.api.services;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * Asks the static site to rebuild after content it renders has changed.
 */
public interface DeployHookService {
    /**
     * Best-effort and asynchronous: never throws and never delays the caller. Inside a
     * transaction the hook fires only after it commits, so a rolled-back edit triggers nothing.
     */
    void contentChanged(String reason);
}
