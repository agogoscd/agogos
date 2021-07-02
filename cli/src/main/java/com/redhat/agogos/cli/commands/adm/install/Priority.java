package com.redhat.agogos.cli.commands.adm.install;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Priority {
    @Nonbinding
    public int value() default 999;
}
