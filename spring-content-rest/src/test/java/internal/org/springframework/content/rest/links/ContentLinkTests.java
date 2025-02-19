package internal.org.springframework.content.rest.links;

import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;
import lombok.Setter;
import org.hamcrest.beans.HasPropertyWithValue;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.data.repository.CrudRepository;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.io.StringReader;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Setter
public class ContentLinkTests {

	private MockMvc mvc;

	private CrudRepository repository;
	private ContentStore store;

	private Object testEntity;
	private String url;
	private String contextPath = "";
	private String linkRel;
	private String expectedLinkRegex;

	{
		Context("given content is associated", () -> {
			BeforeEach(() -> {
			});
			Context("a GET to /{api}?/{repository}/{id}", () -> {
				It("should provide a response with a content link", () -> {
					MockHttpServletResponse response = mvc.perform(get(url)
									.accept("application/hal+json")
									.contextPath(contextPath))
							.andExpect(status().isOk()).andReturn().getResponse();
					assertThat(response, is(not(nullValue())));

					RepresentationFactory representationFactory = new StandardRepresentationFactory();
					ReadableRepresentation halResponse = representationFactory
							.readRepresentation("application/hal+json",
									new StringReader(response.getContentAsString()));

					assertThat(halResponse, is(not(nullValue())));
					assertThat(halResponse.getLinksByRel(linkRel), is(not(nullValue())));
					assertThat(halResponse.getLinksByRel(linkRel), hasItems(new HasPropertyWithValue("href", matchesPattern(expectedLinkRegex))));
				});
			});
		});
	}
}
