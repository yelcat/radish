package com.radishframework.radish.springboot.tracing.mybatis;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class,
                ResultHandler.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class MyBatisTracingIntercepter implements Interceptor {

    public static final Logger logger = LoggerFactory.getLogger(MyBatisTracingIntercepter.class);

    private static final String TYPE = "sql";

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        Tracer tracer = GlobalTracer.get();
        Span activeSpan = tracer.activeSpan();
        if (null != activeSpan) {
            Span span = tracer.buildSpan(sqlCommandType.toString() + "/" + mappedStatement.getId()).start();
            span.setTag(Tags.DB_TYPE, TYPE);
            span.setTag("class.resource", mappedStatement.getResource());
            span.setTag("class.method", mappedStatement.getId());
            Object result = invocation.proceed();
            span.finish();
            return result;
        } else {
            return invocation.proceed();
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
}
