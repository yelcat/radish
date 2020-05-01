package com.radishframework.radish.springboot.tracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.HashMap;
import java.util.Map;

@Aspect
public class RadishTraceAspect {

    @Around("@annotation(com.radishframework.radish.springboot.annotations.RadishTrace)")
    public Object doAround(ProceedingJoinPoint joinPoint) {
        final Tracer tracer = GlobalTracer.get();
        Span span = tracer.buildSpan(joinPoint.getSignature().getName()).start();
        try (Scope scope = tracer.scopeManager().activate(span)) {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                throw new ProceedException(throwable);
            }
        } catch (ProceedException e) {
            throw e;
        } catch (Exception ex) {
            Tags.ERROR.set(span, true);
            Map<String, Object> logMap = new HashMap<>();
            logMap.put(Fields.EVENT, "error");
            logMap.put(Fields.ERROR_OBJECT, ex);
            logMap.put(Fields.MESSAGE, ex.getMessage());
            span.log(logMap);
            return null;
        } finally {
            span.finish();
        }
    }

    static class ProceedException extends RuntimeException {

        ProceedException(Throwable throwable) {
            super(throwable);
        }
    }
}
