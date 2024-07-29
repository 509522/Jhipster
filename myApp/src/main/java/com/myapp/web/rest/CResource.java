package com.myapp.web.rest;

import com.myapp.domain.C;
import com.myapp.repository.CRepository;
import com.myapp.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.reactive.ResponseUtil;

/**
 * REST controller for managing {@link com.myapp.domain.C}.
 */
@RestController
@RequestMapping("/api/cs")
@Transactional
public class CResource {

    private static final Logger log = LoggerFactory.getLogger(CResource.class);

    private static final String ENTITY_NAME = "c";

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
    public Mono<ResponseEntity<C>> createC(@RequestBody C c) throws URISyntaxException {
        log.debug("REST request to save C : {}", c);
        if (c.getId() != null) {
            throw new BadRequestAlertException("A new c cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return cRepository
            .save(c)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/cs/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * {@code GET  /cs} : get all the cS.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of cS in body.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<C>> getAllCS() {
        log.debug("REST request to get all CS");
        return cRepository.findAll().collectList();
    }

    /**
     * {@code GET  /cs} : get all the cS as a stream.
     * @return the {@link Flux} of cS.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<C> getAllCSAsStream() {
        log.debug("REST request to get all CS as a stream");
        return cRepository.findAll();
    }

    /**
     * {@code GET  /cs/:id} : get the "id" c.
     *
     * @param id the id of the c to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the c, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<C>> getC(@PathVariable("id") Long id) {
        log.debug("REST request to get C : {}", id);
        Mono<C> c = cRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(c);
    }

    /**
     * {@code DELETE  /cs/:id} : delete the "id" c.
     *
     * @param id the id of the c to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteC(@PathVariable("id") Long id) {
        log.debug("REST request to delete C : {}", id);
        return cRepository
            .deleteById(id)
            .then(
                Mono.just(
                    ResponseEntity.noContent()
                        .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
                        .build()
                )
            );
    }
}
