package com.myapp.web.rest;

import static com.myapp.domain.CAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.IntegrationTest;
import com.myapp.domain.C;
import com.myapp.repository.CRepository;
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
 * Integration tests for the {@link CResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
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
    private MockMvc restCMockMvc;

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

    @BeforeEach
    public void initTest() {
        c = createEntity(em);
    }

    @AfterEach
    public void cleanup() {
        if (insertedC != null) {
            cRepository.delete(insertedC);
            insertedC = null;
        }
    }

    @Test
    @Transactional
    void createC() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the C
        var returnedC = om.readValue(
            restCMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(c)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            C.class
        );

        // Validate the C in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertCUpdatableFieldsEquals(returnedC, getPersistedC(returnedC));

        insertedC = returnedC;
    }

    @Test
    @Transactional
    void createCWithExistingId() throws Exception {
        // Create the C with an existing ID
        c.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restCMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(c)))
            .andExpect(status().isBadRequest());

        // Validate the C in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllCS() throws Exception {
        // Initialize the database
        insertedC = cRepository.saveAndFlush(c);

        // Get all the cList
        restCMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(c.getId().intValue())));
    }

    @Test
    @Transactional
    void getC() throws Exception {
        // Initialize the database
        insertedC = cRepository.saveAndFlush(c);

        // Get the c
        restCMockMvc
            .perform(get(ENTITY_API_URL_ID, c.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(c.getId().intValue()));
    }

    @Test
    @Transactional
    void getNonExistingC() throws Exception {
        // Get the c
        restCMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void deleteC() throws Exception {
        // Initialize the database
        insertedC = cRepository.saveAndFlush(c);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the c
        restCMockMvc.perform(delete(ENTITY_API_URL_ID, c.getId()).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return cRepository.count();
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
        return cRepository.findById(c.getId()).orElseThrow();
    }

    protected void assertPersistedCToMatchAllProperties(C expectedC) {
        assertCAllPropertiesEquals(expectedC, getPersistedC(expectedC));
    }

    protected void assertPersistedCToMatchUpdatableProperties(C expectedC) {
        assertCAllUpdatablePropertiesEquals(expectedC, getPersistedC(expectedC));
    }
}
