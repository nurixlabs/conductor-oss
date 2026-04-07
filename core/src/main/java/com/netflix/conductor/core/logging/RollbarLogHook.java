/*
 *  Copyright 2023 Conductor authors
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.core.logging;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rollbar.notifier.Rollbar;

public class RollbarLogHook implements LogHook {

    private static final Logger LOGGER = LoggerFactory.getLogger(RollbarLogHook.class);
    private final Rollbar rollbar;

    public RollbarLogHook(Rollbar rollbar) {
        this.rollbar = rollbar;
    }

    @Override
    public void onError(String message, Throwable throwable) {
        if (rollbar == null) return;
        try {
            if (throwable != null) rollbar.error(throwable, message);
            else rollbar.error(message);
        } catch (Exception e) {
            LOGGER.warn("Failed to report error to Rollbar: {}", e.getMessage());
        }
    }

    @Override
    public void onError(String message, Throwable throwable, Map<String, Object> custom) {
        if (rollbar == null) return;
        try {
            if (throwable != null) rollbar.error(throwable, custom, message);
            else rollbar.error(message, custom);
        } catch (Exception e) {
            LOGGER.warn("Failed to report error to Rollbar: {}", e.getMessage());
        }
    }
}
