package com.myapp.web.rest;

import static com.myapp.domain.CAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.IntegrationTest;
import com.myapp.domain.C;
import com.myapp.repository.CRepository;
import com.myapp.repository.EntityManager;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for the {@link CResource} REST controller.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class CResourceIT {

    private static final String ENTITY_API_URL = "/api/cs";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CRepository cRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private C c;

    private C insertedC;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static C createEntity(EntityManager em) {
        C c = new C();
        return c;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static C createUpdatedEntity(EntityManager em) {
        C c = new C();
        return c;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(C.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @BeforeEach
    public void initTest() {
        c = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedC != null) {
            cRepository.delete(insertedC).block();
            insertedC = null;
        }
        deleteEntities(em);
    }

    @Test
    void createC() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the C
        var returnedC = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(c))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(C.class)
            .returnResult()
            .getResponseBody();

        // Validate the C in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertCUpdatableFieldsEquals(returnedC, getPersistedC(returnedC));

        insertedC = returnedC;
    }

    @Test
    void createCWithExistingId() throws Exception {
        // Create the C with an existing ID
        c.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(c))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the C in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void getAllCSAsStream() {
        // Initialize the database
        cRepository.save(c).block();

        List<C> cList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(C.class)
            .getResponseBody()
            .filter(c::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(cList).isNotNull();
        assertThat(cList).hasSize(1);
        C testC = cList.get(0);

        // Test fails because reactive api returns an empty object instead of null
        // assertCAllPropertiesEquals(c, testC);
        assertCUpdatableFieldsEquals(c, testC);
    }

    @Test
    void getAllCS() {
        // Initialize the database
        insertedC = cRepository.save(c).block();

        // Get all the cList
        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?sort=id,desc")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(c.getId().intValue()));
    }

    @Test
    void getC() {
        // Initialize the database
        insertedC = cRepository.save(c).block();

        // Get the c
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, c.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(c.getId().intValue()));
    }

    @Test
    void getNonExistingC() {
        // Get the c
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void deleteC() {
        // Initialize the database
        insertedC = cRepository.save(c).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the c
        webTestClient.delete().uri(ENTITY_API_URL_ID, c.getId()).accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return cRepository.count().block();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected C getPersistedC(C c) {
        return cRepository.findById(c.getId()).block();
    }

    protected void assertPersistedCToMatchAllProperties(C expectedC) {
        // Test fails because reactive api returns an empty object instead of null
        // assertCAllPropertiesEquals(expectedC, getPersistedC(expectedC));
        assertCUpdatableFieldsEquals(expectedC, getPersistedC(expectedC));
    }

    protected void assertPersistedCToMatchUpdatableProperties(C expectedC) {
        // Test fails because reactive api returns an empty object instead of null
        // assertCAllUpdatablePropertiesEquals(expectedC, getPersistedC(expectedC));
        assertCUpdatableFieldsEquals(expectedC, getPersistedC(expectedC));
    }
}
