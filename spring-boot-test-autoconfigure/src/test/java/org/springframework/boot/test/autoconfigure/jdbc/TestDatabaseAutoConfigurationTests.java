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

package org.springframework.boot.test.autoconfigure.jdbc;

import javax.sql.DataSource;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ApplicationContextTester;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestDatabaseAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
public class TestDatabaseAutoConfigurationTests {

	private final ApplicationContextTester context = new ApplicationContextTester()
			.withConfiguration(
					AutoConfigurations.of(TestDatabaseAutoConfiguration.class));

	@Test
	public void replaceWithNoDataSourceAvailable() {
		this.context
				.run((loaded) -> assertThat(loaded).doesNotHaveBean(DataSource.class));
	}

	@Test
	public void replaceWithUniqueDatabase() {
		this.context.withUserConfiguration(ExistingDataSourceConfiguration.class)
				.run((loaded) -> {
					DataSource datasource = loaded.getBean(DataSource.class);
					JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
					jdbcTemplate.execute("create table example (id int, name varchar);");
					this.context.run((secondContext) -> {
						DataSource anotherDatasource = secondContext
								.getBean(DataSource.class);
						JdbcTemplate anotherJdbcTemplate = new JdbcTemplate(
								anotherDatasource);
						anotherJdbcTemplate
								.execute("create table example (id int, name varchar);");
					});
				});
	}

	@Configuration
	static class ExistingDataSourceConfiguration {

		@Bean
		public DataSource dataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder()
					.generateUniqueName(true).setType(EmbeddedDatabaseType.HSQL);
			return builder.build();
		}

	}

}
