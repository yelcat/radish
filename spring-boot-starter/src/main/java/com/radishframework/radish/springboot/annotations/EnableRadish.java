package com.radishframework.radish.springboot.annotations;

import com.radishframework.radish.springboot.RadishConfiguration;
import com.radishframework.radish.springboot.server.SpringBootServerRunner;
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
