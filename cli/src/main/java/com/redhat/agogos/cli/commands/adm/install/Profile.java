package com.redhat.agogos.cli.commands.adm.install;

import com.redhat.agogos.cli.commands.adm.InstallCommand.InstallProfile;

import javax.inject.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Qualifier
@Repeatable(Profiles.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD })
public @interface Profile {
    public InstallProfile value();
}
