package com.BlackDot.Finance.Tracker.Auth;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@auth.isSelf(#id)")
public @interface SelfOnly {}


