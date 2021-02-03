package org.springframework.demo.ioc.annotation.config.conditional;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author Jie Zhao
 * @date 2020/11/3 20:02
 */
@Configuration
@Conditional(ConditionalConfiguration.SystemCondition.class)
public class ConditionalConfiguration {



	public static class SystemCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			String property = context.getEnvironment().getProperty("os.name");


			return false;
		}
	}

}
