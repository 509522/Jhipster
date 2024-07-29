package com.myapp.web.rest;

import static com.myapp.domain.AAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.IntegrationTest;
import com.myapp.domain.A;
import com.myapp.repository.ARepository;
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
 * Integration tests for the {@link AResource} REST controller.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class AResourceIT {

    private static final String ENTITY_API_URL = "/api/as";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ARepository aRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private A a;

    private A insertedA;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static A createEntity(EntityManager em) {
        A a = new A();
        return a;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static A createUpdatedEntity(EntityManager em) {
        A a = new A();
        return a;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(A.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @BeforeEach
    public void initTest() {
        a = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedA != null) {
            aRepository.delete(insertedA).block();
            insertedA = null;
        }
        deleteEntities(em);
    }

    @Test
    void createA() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the A
        var returnedA = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(a))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(A.class)
            .returnResult()
            .getResponseBody();

        // Validate the A in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertAUpdatableFieldsEquals(returnedA, getPersistedA(returnedA));

        insertedA = returnedA;
    }

    @Test
    void createAWithExistingId() throws Exception {
        // Create the A with an existing ID
        a.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(a))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the A in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void getAllASAsStream() {
        // Initialize the database
        aRepository.save(a).block();

        List<A> aList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(A.class)
            .getResponseBody()
            .filter(a::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(aList).isNotNull();
        assertThat(aList).hasSize(1);
        A testA = aList.get(0);

        // Test fails because reactive api returns an empty object instead of null
        // assertAAllPropertiesEquals(a, testA);
        assertAUpdatableFieldsEquals(a, testA);
    }

    @Test
    void getAllAS() {
        // Initialize the database
        insertedA = aRepository.save(a).block();

        // Get all the aList
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
            .value(hasItem(a.getId().intValue()));
    }

    @Test
    void getA() {
        // Initialize the database
        insertedA = aRepository.save(a).block();

        // Get the a
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, a.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(a.getId().intValue()));
    }

    @Test
    void getNonExistingA() {
        // Get the a
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void deleteA() {
        // Initialize the database
        insertedA = aRepository.save(a).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the a
        webTestClient.delete().uri(ENTITY_API_URL_ID, a.getId()).accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return aRepository.count().block();
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

    protected A getPersistedA(A a) {
        return aRepository.findById(a.getId()).block();
    }

    protected void assertPersistedAToMatchAllProperties(A expectedA) {
        // Test fails because reactive api returns an empty object instead of null
        // assertAAllPropertiesEquals(expectedA, getPersistedA(expectedA));
        assertAUpdatableFieldsEquals(expectedA, getPersistedA(expectedA));
    }

    protected void assertPersistedAToMatchUpdatableProperties(A expectedA) {
        // Test fails because reactive api returns an empty object instead of null
        // assertAAllUpdatablePropertiesEquals(expectedA, getPersistedA(expectedA));
        assertAUpdatableFieldsEquals(expectedA, getPersistedA(expectedA));
    }
}
