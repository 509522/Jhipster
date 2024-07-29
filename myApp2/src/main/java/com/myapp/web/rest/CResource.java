package com.myapp.web.rest;

import com.myapp.domain.C;
import com.myapp.repository.CRepository;
import com.myapp.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.myapp.domain.C}.
 */
@RestController
@RequestMapping("/api/cs")
@Transactional
public class CResource {

    private static final Logger log = LoggerFactory.getLogger(CResource.class);

    private static final String ENTITY_NAME = "myApp2C";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final CRepository cRepository;

    public CResource(CRepository cRepository) {
        this.cRepository = cRepository;
    }

    /**
     * {@code POST  /cs} : Create a new c.
     *
     * @param c the c to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new c, or with status {@code 400 (Bad Request)} if the c has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<C> createC(@RequestBody C c) throws URISyntaxException {
        log.debug("REST request to save C : {}", c);
        if (c.getId() != null) {
            throw new BadRequestAlertException("A new c cannot already have an ID", ENTITY_NAME, "idexists");
        }
        c = cRepository.save(c);
        return ResponseEntity.created(new URI("/api/cs/" + c.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, c.getId().toString()))
            .body(c);
    }

    /**
     * {@code GET  /cs} : get all the cS.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of cS in body.
     */
    @GetMapping("")
    public List<C> getAllCS() {
        log.debug("REST request to get all CS");
        return cRepository.findAll();
    }

    /**
     * {@code GET  /cs/:id} : get the "id" c.
     *
     * @param id the id of the c to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the c, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<C> getC(@PathVariable("id") Long id) {
        log.debug("REST request to get C : {}", id);
        Optional<C> c = cRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(c);
    }

    /**
     * {@code DELETE  /cs/:id} : delete the "id" c.
     *
     * @param id the id of the c to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteC(@PathVariable("id") Long id) {
        log.debug("REST request to delete C : {}", id);
        cRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
