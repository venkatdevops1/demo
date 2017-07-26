/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.context;

import java.util.function.Supplier;

import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * A {@link AbstractApplicationContextTester ApplicationContext tester} for a Servlet
 * based {@link ConfigurableWebApplicationContext}.
 * <p>
 * See {@link AbstractApplicationContextTester} for details.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class WebApplicationContextTester extends
		AbstractApplicationContextTester<WebApplicationContextTester, ConfigurableWebApplicationContext, AssertableWebApplicationContext> {

	/**
	 * Create a new {@link WebApplicationContextTester} instance using an
	 * {@link AnnotationConfigWebApplicationContext} with a {@link MockServletContext} as
	 * the underlying source.
	 * @see #withMockServletContext(Supplier)
	 */
	public WebApplicationContextTester() {
		this(withMockServletContext(AnnotationConfigWebApplicationContext::new));
	}

	/**
	 * Create a new {@link WebApplicationContextTester} instance using the specified
	 * {@code contextFactory} as the underlying source.
	 * @param contextFactory a supplier that returns a new instance on each call
	 */
	public WebApplicationContextTester(
			Supplier<ConfigurableWebApplicationContext> contextFactory) {
		super(contextFactory);
	}

	/**
	 * Decorate the specified {@code contextFactory} to set a {@link MockServletContext}
	 * on each newly created {@link WebApplicationContext}.
	 * @param contextFactory the context factory to decorate
	 * @return an updated supplier that will set the {@link MockServletContext}
	 */
	public static Supplier<ConfigurableWebApplicationContext> withMockServletContext(
			Supplier<ConfigurableWebApplicationContext> contextFactory) {
		return (contextFactory == null ? null : () -> {
			ConfigurableWebApplicationContext context = contextFactory.get();
			context.setServletContext(new MockServletContext());
			return context;
		});
	}

}
