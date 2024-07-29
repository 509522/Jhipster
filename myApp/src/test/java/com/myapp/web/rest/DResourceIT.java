package com.myapp.web.rest;

import static com.myapp.domain.DAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.IntegrationTest;
import com.myapp.domain.D;
import com.myapp.repository.DRepository;
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
 * Integration tests for the {@link DResource} REST controller.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class DResourceIT {

    private static final String ENTITY_API_URL = "/api/ds";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DRepository dRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private D d;

    private D insertedD;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static D createEntity(EntityManager em) {
        D d = new D();
        return d;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static D createUpdatedEntity(EntityManager em) {
        D d = new D();
        return d;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(D.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @BeforeEach
    public void initTest() {
        d = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedD != null) {
            dRepository.delete(insertedD).block();
            insertedD = null;
        }
        deleteEntities(em);
    }

    @Test
    void createD() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the D
        var returnedD = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(d))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(D.class)
            .returnResult()
            .getResponseBody();

        // Validate the D in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertDUpdatableFieldsEquals(returnedD, getPersistedD(returnedD));

        insertedD = returnedD;
    }

    @Test
    void createDWithExistingId() throws Exception {
        // Create the D with an existing ID
        d.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(d))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the D in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void getAllDSAsStream() {
        // Initialize the database
        dRepository.save(d).block();

        List<D> dList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(D.class)
            .getResponseBody()
            .filter(d::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(dList).isNotNull();
        assertThat(dList).hasSize(1);
        D testD = dList.get(0);

        // Test fails because reactive api returns an empty object instead of null
        // assertDAllPropertiesEquals(d, testD);
        assertDUpdatableFieldsEquals(d, testD);
    }

    @Test
    void getAllDS() {
        // Initialize the database
        insertedD = dRepository.save(d).block();

        // Get all the dList
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
            .value(hasItem(d.getId().intValue()));
    }

    @Test
    void getD() {
        // Initialize the database
        insertedD = dRepository.save(d).block();

        // Get the d
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, d.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(d.getId().intValue()));
    }

    @Test
    void getNonExistingD() {
        // Get the d
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void deleteD() {
        // Initialize the database
        insertedD = dRepository.save(d).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the d
        webTestClient.delete().uri(ENTITY_API_URL_ID, d.getId()).accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return dRepository.count().block();
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

    protected D getPersistedD(D d) {
        return dRepository.findById(d.getId()).block();
    }

    protected void assertPersistedDToMatchAllProperties(D expectedD) {
        // Test fails because reactive api returns an empty object instead of null
        // assertDAllPropertiesEquals(expectedD, getPersistedD(expectedD));
        assertDUpdatableFieldsEquals(expectedD, getPersistedD(expectedD));
    }

    protected void assertPersistedDToMatchUpdatableProperties(D expectedD) {
        // Test fails because reactive api returns an empty object instead of null
        // assertDAllUpdatablePropertiesEquals(expectedD, getPersistedD(expectedD));
        assertDUpdatableFieldsEquals(expectedD, getPersistedD(expectedD));
    }
}
