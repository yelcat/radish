package com.radishframework.radish.server.annotations;

import com.radishframework.radish.server.springboot.RadishConfiguration;
import com.radishframework.radish.server.springboot.SpringBootServerRunner;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({
        RadishConfiguration.class,
        SpringBootServerRunner.class,
})
public @interface EnableRadish {

}
