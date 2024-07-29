package com.myapp.web.rest;

import static com.myapp.domain.AAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.IntegrationTest;
import com.myapp.domain.A;
import com.myapp.repository.ARepository;
import jakarta.persistence.EntityManager;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link AResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
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
    private MockMvc restAMockMvc;

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

    @BeforeEach
    public void initTest() {
        a = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedA != null) {
            aRepository.delete(insertedA);
            insertedA = null;
        }
    }

    @Test
    @Transactional
    void createA() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the A
        var returnedA = om.readValue(
            restAMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(a)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            A.class
        );

        // Validate the A in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertAUpdatableFieldsEquals(returnedA, getPersistedA(returnedA));

        insertedA = returnedA;
    }

    @Test
    @Transactional
    void createAWithExistingId() throws Exception {
        // Create the A with an existing ID
        a.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(a)))
            .andExpect(status().isBadRequest());

        // Validate the A in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllAS() throws Exception {
        // Initialize the database
        insertedA = aRepository.saveAndFlush(a);

        // Get all the aList
        restAMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(a.getId().intValue())));
    }

    @Test
    @Transactional
    void getA() throws Exception {
        // Initialize the database
        insertedA = aRepository.saveAndFlush(a);

        // Get the a
        restAMockMvc
            .perform(get(ENTITY_API_URL_ID, a.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(a.getId().intValue()));
    }

    @Test
    @Transactional
    void getNonExistingA() throws Exception {
        // Get the a
        restAMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void deleteA() throws Exception {
        // Initialize the database
        insertedA = aRepository.saveAndFlush(a);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the a
        restAMockMvc.perform(delete(ENTITY_API_URL_ID, a.getId()).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return aRepository.count();
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
        return aRepository.findById(a.getId()).orElseThrow();
    }

    protected void assertPersistedAToMatchAllProperties(A expectedA) {
        assertAAllPropertiesEquals(expectedA, getPersistedA(expectedA));
    }

    protected void assertPersistedAToMatchUpdatableProperties(A expectedA) {
        assertAAllUpdatablePropertiesEquals(expectedA, getPersistedA(expectedA));
    }
}
