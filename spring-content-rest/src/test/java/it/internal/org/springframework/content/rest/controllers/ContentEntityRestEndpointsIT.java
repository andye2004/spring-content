package it.internal.org.springframework.content.rest.controllers;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

import internal.org.springframework.content.rest.support.EntityConfig;
import internal.org.springframework.content.rest.support.StoreConfig;
import internal.org.springframework.content.rest.support.TestEntity;
import internal.org.springframework.content.rest.support.TestEntity3;
import internal.org.springframework.content.rest.support.TestEntity3ContentRepository;
import internal.org.springframework.content.rest.support.TestEntity3Repository;
import internal.org.springframework.content.rest.support.TestEntity4;
import internal.org.springframework.content.rest.support.TestEntity4ContentRepository;
import internal.org.springframework.content.rest.support.TestEntity4Repository;
import internal.org.springframework.content.rest.support.TestEntity6;
import internal.org.springframework.content.rest.support.TestEntity6Repository;
import internal.org.springframework.content.rest.support.TestEntity6Store;
import internal.org.springframework.content.rest.support.TestEntity9;
import internal.org.springframework.content.rest.support.TestEntity9Repository;
import internal.org.springframework.content.rest.support.TestEntity9Store;
import internal.org.springframework.content.rest.support.TestEntityContentRepository;
import internal.org.springframework.content.rest.support.TestEntityRepository;
import internal.org.springframework.content.rest.support.TestStore;

@RunWith(Ginkgo4jSpringRunner.class)
// @Ginkgo4jConfiguration(threads=1)
@WebAppConfiguration
@ContextConfiguration(classes = {
		StoreConfig.class,
		EntityConfig.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class })
@Transactional
@ActiveProfiles("store")
public class ContentEntityRestEndpointsIT {

	// different exported URI
	@Autowired
	TestEntityRepository repository;
	@Autowired
	TestEntityContentRepository contentRepository;

	// same exported URI
	@Autowired
	TestEntity3Repository repo3;
	@Autowired
	TestEntity3ContentRepository store3;

	// same exported URI
	@Autowired
	TestEntity4Repository repo4;
	@Autowired
	TestEntity4ContentRepository store4;

	// shared @Id/@ContentId
	@Autowired
	TestEntity6Repository repo6;
	@Autowired
	TestEntity6Store store6;

    // single content property, correlated attributes
    @Autowired
    TestEntity9Repository repo9;
    @Autowired
    TestEntity9Store store9;

	@Autowired
	TestStore store;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	private TestEntity testEntity;
	private TestEntity3 testEntity3;
	private TestEntity4 testEntity4;
	private TestEntity6 testEntity6;
    private TestEntity9 testEntity9;

	private Version version;
	private LastModifiedDate lastModifiedDate;

	private Entity entityTests;
	private Content contentTests;
	private Cors corsTests;

	{
		Describe("Content Entity REST Endpoints", () -> {
			BeforeEach(() -> {
				mvc = MockMvcBuilders.webAppContextSetup(context).build();
			});
			Context("given an entity is the subject of a repository and storage", () -> {
				Context("given the repository and storage are exported to the same URI", () -> {
					BeforeEach(() -> {
						testEntity3 = repo3.save(new TestEntity3());
						testEntity3.name = "tests";
						testEntity3 = repo3.save(testEntity3);

						entityTests.setMvc(mvc);
						entityTests.setUrl("/testEntity3s/" + testEntity3.id);
						entityTests.setEntity(testEntity3);
						entityTests.setRepository(repo3);
						entityTests.setLinkRel("testEntity");

						contentTests.setMvc(mvc);
						contentTests.setUrl("/testEntity3s/" + testEntity3.getId());
						contentTests.setEntity(testEntity3);
						contentTests.setRepository(repo3);
						contentTests.setStore(store3);

					});
					entityTests = Entity.tests();
					contentTests = Content.tests();

					Context("a DELETE to /{store}/{id}/softDelete (custom handler)", () -> {
			           It("should return 200", () -> {
			                mvc.perform(delete("/testEntity3s/" + testEntity3.id + "/softDelete"))
			                        .andExpect(status().is2xxSuccessful());
			            });
			        });
				});
				Context("given the repository and storage are exported to different URIs", () -> {
					BeforeEach(() -> {
						testEntity = repository.save(new TestEntity());

						contentTests.setMvc(mvc);
						contentTests.setUrl("/testEntitiesContent/" + testEntity.getId());
						contentTests.setEntity(testEntity);
						contentTests.setRepository(repository);
						contentTests.setStore(contentRepository);

						corsTests.setMvc(mvc);
						corsTests.setUrl("/testEntitiesContent/" + testEntity.getId());
					});
					contentTests = Content.tests();
					corsTests = Cors.tests();

					//////////////////////////////////////////////
					// Temporary test for testing spring data rest cors configurations
					//
					Context("an OPTIONS request to the repository from a known host", () -> {
						It("should return the relevant CORS headers and OK", () -> {
							mvc.perform(options("/testEntities/" + testEntity.getId())
									.header("Access-Control-Request-Method", "PUT")
									.header("Origin", "http://www.someurl.com"))
									.andExpect(status().isOk())
									.andExpect(header().string("Access-Control-Allow-Origin","http://www.someurl.com"));
						});
					});
					//
					//////////////////////////////////////////////

				});
				Context("given an entity with @Version", () -> {
					BeforeEach(() -> {
						testEntity4 = new TestEntity4();
						testEntity4 = store4.setContent(testEntity4, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
						testEntity4.mimeType = "text/plain";
						testEntity4 = repo4.save(testEntity4);
						String url = "/testEntity4s/" + testEntity4.getId();

						version.setMvc(mvc);
						version.setUrl(url);
						version.setRepo(repo4);
						version.setStore(store4);
						version.setEtag(format("\"%s\"", testEntity4.getVersion()));
						version.setEntity(testEntity4);
					});
					version = Version.tests();
				});

				Context("given an entity with @LastModifiedDate", () -> {
					BeforeEach(() -> {
						String content = "Hello Spring Content LastModifiedDate World!";

						testEntity4 = new TestEntity4();
						testEntity4 = store4.setContent(testEntity4, new ByteArrayInputStream(content.getBytes()));
						testEntity4.mimeType = "text/plain";
						testEntity4 = repo4.save(testEntity4);
						String url = "/testEntity4s/" + testEntity4.getId();

						lastModifiedDate.setMvc(mvc);
						lastModifiedDate.setUrl(url);
						lastModifiedDate.setLastModifiedDate(testEntity4.getModifiedDate());
						lastModifiedDate.setEtag(testEntity4.getVersion().toString());
						lastModifiedDate.setContent(content);
					});
					lastModifiedDate = LastModifiedDate.tests();
				});

				Context("given an entity with a shared Id and ContentId field", () -> {

					BeforeEach(() -> {
						testEntity6 = new TestEntity6();
						testEntity6 = repo6.save(testEntity6);
					});

					It("should return 404 when no content is set", () -> {
						mvc.perform(get("/testEntity6s/" + testEntity6.getId())
									.accept("text/plain"))
							.andExpect(status().isNotFound());
					});
				});
			});

            Context("given an entity with a single content property of correlated attributes", () -> {
                BeforeEach(() -> {
                    testEntity9 = repo9.save(new TestEntity9());
                });
                It("should support content operations", () -> {
                    String content = "Hello Spring Content World!";
                    mvc.perform(
                            put("/testEntity9s/" + testEntity9.id)
                            .contextPath("")
                            .content(content)
                            .contentType("text/plain"))
                    .andExpect(status().isCreated());

                    Optional<TestEntity9> fetched = repo9.findById(testEntity9.getId());
                    assertThat(fetched.isPresent(), is(true));
                    assertThat(fetched.get().getContentId(), is(not(nullValue())));
                    assertThat(fetched.get().getContentLen(), is(27L));
                    assertThat(fetched.get().getContentMimeType(), is("text/plain"));
                    assertThat(IOUtils.toString(store9.getContent(fetched.get()), Charset.defaultCharset()), is(content));

                    MockHttpServletResponse response = mvc
                            .perform(get("/testEntity9s/" + testEntity9.id)
                                    .contextPath("")
                                    .accept("text/plain"))
                            .andExpect(status().isOk()).andReturn().getResponse();

                    assertThat(response, is(not(nullValue())));
                    assertThat(response.getContentAsString(), is("Hello Spring Content World!"));

                });
            });
		});
	}

	@Test
	public void noop() {
	}
}