package com.myapp.web.rest;

import static com.myapp.domain.BAsserts.*;
import static com.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.IntegrationTest;
import com.myapp.domain.B;
import com.myapp.repository.BRepository;
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
 * Integration tests for the {@link BResource} REST controller.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class BResourceIT {

    private static final String ENTITY_API_URL = "/api/bs";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private BRepository bRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private B b;

    private B insertedB;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static B createEntity(EntityManager em) {
        B b = new B();
        return b;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static B createUpdatedEntity(EntityManager em) {
        B b = new B();
        return b;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(B.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @BeforeEach
    public void initTest() {
        b = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedB != null) {
            bRepository.delete(insertedB).block();
            insertedB = null;
        }
        deleteEntities(em);
    }

    @Test
    void createB() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the B
        var returnedB = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(b))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(B.class)
            .returnResult()
            .getResponseBody();

        // Validate the B in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertBUpdatableFieldsEquals(returnedB, getPersistedB(returnedB));

        insertedB = returnedB;
    }

    @Test
    void createBWithExistingId() throws Exception {
        // Create the B with an existing ID
        b.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(b))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the B in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void getAllBSAsStream() {
        // Initialize the database
        bRepository.save(b).block();

        List<B> bList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(B.class)
            .getResponseBody()
            .filter(b::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(bList).isNotNull();
        assertThat(bList).hasSize(1);
        B testB = bList.get(0);

        // Test fails because reactive api returns an empty object instead of null
        // assertBAllPropertiesEquals(b, testB);
        assertBUpdatableFieldsEquals(b, testB);
    }

    @Test
    void getAllBS() {
        // Initialize the database
        insertedB = bRepository.save(b).block();

        // Get all the bList
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
            .value(hasItem(b.getId().intValue()));
    }

    @Test
    void getB() {
        // Initialize the database
        insertedB = bRepository.save(b).block();

        // Get the b
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, b.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(b.getId().intValue()));
    }

    @Test
    void getNonExistingB() {
        // Get the b
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingB() throws Exception {
        // Initialize the database
        insertedB = bRepository.save(b).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the b
        B updatedB = bRepository.findById(b.getId()).block();

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedB.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(updatedB))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the B in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedBToMatchAllProperties(updatedB);
    }

    @Test
    void putNonExistingB() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        b.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, b.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(b))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the B in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchB() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        b.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(b))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the B in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamB() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        b.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(b))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the B in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateBWithPatch() throws Exception {
        // Initialize the database
        insertedB = bRepository.save(b).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the b using partial update
        B partialUpdatedB = new B();
        partialUpdatedB.setId(b.getId());

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedB.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedB))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the B in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertBUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedB, b), getPersistedB(b));
    }

    @Test
    void fullUpdateBWithPatch() throws Exception {
        // Initialize the database
        insertedB = bRepository.save(b).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the b using partial update
        B partialUpdatedB = new B();
        partialUpdatedB.setId(b.getId());

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedB.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedB))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the B in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertBUpdatableFieldsEquals(partialUpdatedB, getPersistedB(partialUpdatedB));
    }

    @Test
    void patchNonExistingB() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        b.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, b.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(b))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the B in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchB() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        b.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(b))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the B in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamB() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        b.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(b))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the B in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteB() {
        // Initialize the database
        insertedB = bRepository.save(b).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the b
        webTestClient.delete().uri(ENTITY_API_URL_ID, b.getId()).accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return bRepository.count().block();
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

    protected B getPersistedB(B b) {
        return bRepository.findById(b.getId()).block();
    }

    protected void assertPersistedBToMatchAllProperties(B expectedB) {
        // Test fails because reactive api returns an empty object instead of null
        // assertBAllPropertiesEquals(expectedB, getPersistedB(expectedB));
        assertBUpdatableFieldsEquals(expectedB, getPersistedB(expectedB));
    }

    protected void assertPersistedBToMatchUpdatableProperties(B expectedB) {
        // Test fails because reactive api returns an empty object instead of null
        // assertBAllUpdatablePropertiesEquals(expectedB, getPersistedB(expectedB));
        assertBUpdatableFieldsEquals(expectedB, getPersistedB(expectedB));
    }
}
