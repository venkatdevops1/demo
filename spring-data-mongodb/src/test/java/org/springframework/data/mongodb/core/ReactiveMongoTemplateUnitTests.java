/*
 * Copyright 2016-2017 the original author or authors.
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
package org.springframework.data.mongodb.core;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

import lombok.Data;
import reactor.core.publisher.Mono;

import org.bson.Document;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplateUnitTests.AutogenerateableId;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.util.ReflectionTestUtils;

import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.reactivestreams.client.FindPublisher;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;

/**
 * Unit tests for {@link ReactiveMongoTemplate}.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ReactiveMongoTemplateUnitTests {

	ReactiveMongoTemplate template;

	@Mock SimpleReactiveMongoDatabaseFactory factory;
	@Mock MongoClient mongoClient;
	@Mock MongoDatabase db;
	@Mock MongoCollection collection;
	@Mock FindPublisher findPublisher;
	@Mock Publisher runCommandPublisher;

	MongoExceptionTranslator exceptionTranslator = new MongoExceptionTranslator();
	MappingMongoConverter converter;
	MongoMappingContext mappingContext;

	@Before
	public void setUp() {

		when(factory.getExceptionTranslator()).thenReturn(exceptionTranslator);
		when(factory.getMongoDatabase()).thenReturn(db);
		when(db.getCollection(any())).thenReturn(collection);
		when(db.getCollection(any(), any())).thenReturn(collection);
		when(db.runCommand(any(), any(Class.class))).thenReturn(runCommandPublisher);
		when(collection.find()).thenReturn(findPublisher);
		when(collection.find(Mockito.any(Document.class))).thenReturn(findPublisher);
		when(findPublisher.projection(any())).thenReturn(findPublisher);
		when(findPublisher.limit(anyInt())).thenReturn(findPublisher);
		when(findPublisher.collation(any())).thenReturn(findPublisher);
		when(findPublisher.first()).thenReturn(findPublisher);

		this.mappingContext = new MongoMappingContext();
		this.converter = new MappingMongoConverter(new NoOpDbRefResolver(), mappingContext);
		this.template = new ReactiveMongoTemplate(factory, converter);

	}

	@Test(expected = IllegalArgumentException.class) // DATAMONGO-1444
	public void rejectsNullDatabaseName() throws Exception {
		new ReactiveMongoTemplate(mongoClient, null);
	}

	@Test(expected = IllegalArgumentException.class) // DATAMONGO-1444
	public void rejectsNullMongo() throws Exception {
		new ReactiveMongoTemplate(null, "database");
	}

	@Test // DATAMONGO-1444
	public void defaultsConverterToMappingMongoConverter() throws Exception {
		ReactiveMongoTemplate template = new ReactiveMongoTemplate(mongoClient, "database");
		assertTrue(ReflectionTestUtils.getField(template, "mongoConverter") instanceof MappingMongoConverter);
	}

	@Test // DATAMONGO-1518
	public void findShouldUseCollationWhenPresent() {

		template.find(new BasicQuery("{}").collation(Collation.of("fr")), AutogenerateableId.class).subscribe();

		verify(findPublisher).collation(eq(com.mongodb.client.model.Collation.builder().locale("fr").build()));
	}

	//
	@Test // DATAMONGO-1518
	public void findOneShouldUseCollationWhenPresent() {

		template.findOne(new BasicQuery("{}").collation(Collation.of("fr")), AutogenerateableId.class).subscribe();

		verify(findPublisher).collation(eq(com.mongodb.client.model.Collation.builder().locale("fr").build()));
	}

	@Test // DATAMONGO-1518
	public void existsShouldUseCollationWhenPresent() {

		template.exists(new BasicQuery("{}").collation(Collation.of("fr")), AutogenerateableId.class).subscribe();

		verify(findPublisher).collation(eq(com.mongodb.client.model.Collation.builder().locale("fr").build()));
	}

	@Test // DATAMONGO-1518
	public void findAndModfiyShoudUseCollationWhenPresent() {

		when(collection.findOneAndUpdate(any(), any(), any())).thenReturn(Mono.empty());

		template.findAndModify(new BasicQuery("{}").collation(Collation.of("fr")), new Update(), AutogenerateableId.class)
				.subscribe();

		ArgumentCaptor<FindOneAndUpdateOptions> options = ArgumentCaptor.forClass(FindOneAndUpdateOptions.class);
		verify(collection).findOneAndUpdate(Mockito.any(), Mockito.any(), options.capture());

		assertThat(options.getValue().getCollation().getLocale(), is("fr"));
	}

	@Test // DATAMONGO-1518
	public void findAndRemoveShouldUseCollationWhenPresent() {

		when(collection.findOneAndDelete(any(), any())).thenReturn(Mono.empty());

		template.findAndRemove(new BasicQuery("{}").collation(Collation.of("fr")), AutogenerateableId.class).subscribe();

		ArgumentCaptor<FindOneAndDeleteOptions> options = ArgumentCaptor.forClass(FindOneAndDeleteOptions.class);
		verify(collection).findOneAndDelete(Mockito.any(), options.capture());

		assertThat(options.getValue().getCollation().getLocale(), is("fr"));
	}

	@Ignore("see https://jira.mongodb.org/browse/JAVARS-27")
	@Test // DATAMONGO-1518
	public void findAndRemoveManyShouldUseCollationWhenPresent() {

		template.doRemove("collection-1", new BasicQuery("{}").collation(Collation.of("fr")), AutogenerateableId.class)
				.subscribe();

		ArgumentCaptor<DeleteOptions> options = ArgumentCaptor.forClass(DeleteOptions.class);
		// the current mongodb-driver-reactivestreams:1.4.0 driver does not offer deleteMany with options.
		// verify(collection).deleteMany(Mockito.any(), options.capture());

		assertThat(options.getValue().getCollation().getLocale(), is("fr"));
	}

	@Test // DATAMONGO-1518
	public void updateOneShouldUseCollationWhenPresent() {

		when(collection.updateOne(any(), any(), any())).thenReturn(Mono.empty());

		template.updateFirst(new BasicQuery("{}").collation(Collation.of("fr")), new Update().set("foo", "bar"),
				AutogenerateableId.class).subscribe();

		ArgumentCaptor<UpdateOptions> options = ArgumentCaptor.forClass(UpdateOptions.class);
		verify(collection).updateOne(Mockito.any(), Mockito.any(), options.capture());

		assertThat(options.getValue().getCollation().getLocale(), is("fr"));
	}

	@Test // DATAMONGO-1518
	public void updateManyShouldUseCollationWhenPresent() {

		when(collection.updateMany(any(), any(), any())).thenReturn(Mono.empty());

		template.updateMulti(new BasicQuery("{}").collation(Collation.of("fr")), new Update().set("foo", "bar"),
				AutogenerateableId.class).subscribe();

		ArgumentCaptor<UpdateOptions> options = ArgumentCaptor.forClass(UpdateOptions.class);
		verify(collection).updateMany(Mockito.any(), Mockito.any(), options.capture());

		assertThat(options.getValue().getCollation().getLocale(), is("fr"));

	}

	@Test // DATAMONGO-1518
	public void replaceOneShouldUseCollationWhenPresent() {

		when(collection.replaceOne(any(), any(), any())).thenReturn(Mono.empty());

		template.updateFirst(new BasicQuery("{}").collation(Collation.of("fr")), new Update(), AutogenerateableId.class)
				.subscribe();

		ArgumentCaptor<UpdateOptions> options = ArgumentCaptor.forClass(UpdateOptions.class);
		verify(collection).replaceOne(Mockito.any(), Mockito.any(), options.capture());

		assertThat(options.getValue().getCollation().getLocale(), is("fr"));
	}

	@Ignore("currently no aggregation")
	@Test // DATAMONGO-1518
	public void aggregateShouldUseCollationWhenPresent() {

		Aggregation aggregation = newAggregation(project("id"))
				.withOptions(newAggregationOptions().collation(Collation.of("fr")).build());
		// template.aggregate(aggregation, AutogenerateableId.class, Document.class).subscribe();

		ArgumentCaptor<Document> cmd = ArgumentCaptor.forClass(Document.class);
		verify(db).runCommand(cmd.capture(), Mockito.any(Class.class));

		assertThat(cmd.getValue().get("collation", Document.class), equalTo(new Document("locale", "fr")));
	}

	@Ignore("currently no mapReduce")
	@Test // DATAMONGO-1518
	public void mapReduceShouldUseCollationWhenPresent() {

		// template.mapReduce("", "", "", MapReduceOptions.options().collation(Collation.of("fr")),
		// AutogenerateableId.class).subscribe();
		//
		// verify(mapReduceIterable).collation(eq(com.mongodb.client.model.Collation.builder().locale("fr").build()));
	}

	@Test // DATAMONGO-1518
	public void geoNearShouldUseCollationWhenPresent() {

		NearQuery query = NearQuery.near(0D, 0D).query(new BasicQuery("{}").collation(Collation.of("fr")));
		template.geoNear(query, AutogenerateableId.class).subscribe();

		ArgumentCaptor<Document> cmd = ArgumentCaptor.forClass(Document.class);
		verify(db).runCommand(cmd.capture(), Mockito.any(Class.class));

		assertThat(cmd.getValue().get("collation", Document.class), equalTo(new Document("locale", "fr")));
	}

	@Test // DATAMONGO-1719
	public void appliesFieldsWhenInterfaceProjectionIsClosedAndQueryDoesNotDefineFields() {

		template.doFind("star-wars", new Document(), new Document(), Person.class, PersonProjection.class, null)
				.subscribe();

		verify(findPublisher).projection(eq(new Document("firstname", 1)));
	}

	@Test // DATAMONGO-1719
	public void doesNotApplyFieldsWhenInterfaceProjectionIsClosedAndQueryDefinesFields() {

		template.doFind("star-wars", new Document(), new Document("bar", 1), Person.class, PersonProjection.class, null)
				.subscribe();

		verify(findPublisher).projection(eq(new Document("bar", 1)));
	}

	@Test // DATAMONGO-1719
	public void doesNotApplyFieldsWhenInterfaceProjectionIsOpen() {

		template.doFind("star-wars", new Document(), new Document(), Person.class, PersonSpELProjection.class, null)
				.subscribe();

		verify(findPublisher, never()).projection(any());
	}

	@Test // DATAMONGO-1719
	public void doesNotApplyFieldsToDtoProjection() {

		template.doFind("star-wars", new Document(), new Document(), Person.class, Jedi.class, null).subscribe();

		verify(findPublisher, never()).projection(any());
	}

	@Test // DATAMONGO-1719
	public void doesNotApplyFieldsToDtoProjectionWhenQueryDefinesFields() {

		template.doFind("star-wars", new Document(), new Document("bar", 1), Person.class, Jedi.class, null).subscribe();

		verify(findPublisher).projection(eq(new Document("bar", 1)));
	}

	@Test // DATAMONGO-1719
	public void doesNotApplyFieldsWhenTargetIsNotAProjection() {

		template.doFind("star-wars", new Document(), new Document(), Person.class, Person.class, null).subscribe();

		verify(findPublisher, never()).projection(any());
	}

	@Test // DATAMONGO-1719
	public void doesNotApplyFieldsWhenTargetExtendsDomainType() {

		template.doFind("star-wars", new Document(), new Document(), Person.class, PersonExtended.class, null).subscribe();

		verify(findPublisher, never()).projection(any());
	}

	@Data
	@org.springframework.data.mongodb.core.mapping.Document(collection = "star-wars")
	static class Person {

		@Id String id;
		String firstname;
	}

	static class PersonExtended extends Person {

		String lastname;
	}

	interface PersonProjection {
		String getFirstname();
	}

	public interface PersonSpELProjection {

		@Value("#{target.firstname}")
		String getName();
	}

	@Data
	static class Jedi {

		@Field("firstname") String name;
	}
}
