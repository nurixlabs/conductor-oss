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
import org.slf4j.helpers.MessageFormatter;

public class ConductorLogger {

    private static volatile LogHook rollbarHook;
    private final Logger delegate;

    ConductorLogger(Logger delegate) {
        this.delegate = delegate;
    }

    public static void setDefaultLogHook(LogHook hook) {
        rollbarHook = hook;
    }

    public static ConductorLogger getLogger(Class<?> clazz) {
        return new ConductorLogger(LoggerFactory.getLogger(clazz));
    }

    // --- trace/debug/info/warn: delegate directly, no hook ---

    public void trace(String msg) {
        delegate.trace(msg);
    }

    public void trace(String format, Object arg) {
        delegate.trace(format, arg);
    }

    public void trace(String format, Object arg1, Object arg2) {
        delegate.trace(format, arg1, arg2);
    }

    public void trace(String format, Object... arguments) {
        delegate.trace(format, arguments);
    }

    public void trace(String msg, Throwable t) {
        delegate.trace(msg, t);
    }

    public void debug(String msg) {
        delegate.debug(msg);
    }

    public void debug(String format, Object arg) {
        delegate.debug(format, arg);
    }

    public void debug(String format, Object arg1, Object arg2) {
        delegate.debug(format, arg1, arg2);
    }

    public void debug(String format, Object... arguments) {
        delegate.debug(format, arguments);
    }

    public void debug(String msg, Throwable t) {
        delegate.debug(msg, t);
    }

    public void info(String msg) {
        delegate.info(msg);
    }

    public void info(String format, Object arg) {
        delegate.info(format, arg);
    }

    public void info(String format, Object arg1, Object arg2) {
        delegate.info(format, arg1, arg2);
    }

    public void info(String format, Object... arguments) {
        delegate.info(format, arguments);
    }

    public void info(String msg, Throwable t) {
        delegate.info(msg, t);
    }

    public void warn(String msg) {
        delegate.warn(msg);
    }

    public void warn(String format, Object arg) {
        delegate.warn(format, arg);
    }

    public void warn(String format, Object arg1, Object arg2) {
        delegate.warn(format, arg1, arg2);
    }

    public void warn(String format, Object... arguments) {
        delegate.warn(format, arguments);
    }

    public void warn(String msg, Throwable t) {
        delegate.warn(msg, t);
    }

    // --- error: invoke hook first, then SLF4J ---

    public void error(String msg) {
        if (rollbarHook != null) rollbarHook.onError(msg, null);
        delegate.error(msg);
    }

    public void error(String format, Object arg) {
        if (rollbarHook != null)
            rollbarHook.onError(MessageFormatter.format(format, arg).getMessage(), null);
        delegate.error(format, arg);
    }

    public void error(String format, Object arg1, Object arg2) {
        if (rollbarHook != null)
            rollbarHook.onError(MessageFormatter.format(format, arg1, arg2).getMessage(), null);
        delegate.error(format, arg1, arg2);
    }

    public void error(String format, Object... arguments) {
        if (rollbarHook != null)
            rollbarHook.onError(MessageFormatter.arrayFormat(format, arguments).getMessage(), null);
        delegate.error(format, arguments);
    }

    public void error(String msg, Throwable t) {
        if (rollbarHook != null) rollbarHook.onError(msg, t);
        delegate.error(msg, t);
    }

    // Structured metadata overloads — message stays constant for Rollbar grouping,
    // dynamic values go in the map so all occurrences group under one fingerprint.

    public void error(String message, Map<String, Object> custom) {
        if (rollbarHook != null) rollbarHook.onError(message, null, custom);
        delegate.error(message);
    }

    public void error(String message, Throwable t, Map<String, Object> custom) {
        if (rollbarHook != null) rollbarHook.onError(message, t, custom);
        delegate.error(message, t);
    }

    // isXxxEnabled passthrough

    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }
}
