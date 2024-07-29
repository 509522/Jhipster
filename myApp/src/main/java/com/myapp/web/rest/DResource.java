package com.myapp.web.rest;

import com.myapp.domain.D;
import com.myapp.repository.DRepository;
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
 * REST controller for managing {@link com.myapp.domain.D}.
 */
@RestController
@RequestMapping("/api/ds")
@Transactional
public class DResource {

    private static final Logger log = LoggerFactory.getLogger(DResource.class);

    private static final String ENTITY_NAME = "d";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final DRepository dRepository;

    public DResource(DRepository dRepository) {
        this.dRepository = dRepository;
    }

    /**
     * {@code POST  /ds} : Create a new d.
     *
     * @param d the d to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new d, or with status {@code 400 (Bad Request)} if the d has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public Mono<ResponseEntity<D>> createD(@RequestBody D d) throws URISyntaxException {
        log.debug("REST request to save D : {}", d);
        if (d.getId() != null) {
            throw new BadRequestAlertException("A new d cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return dRepository
            .save(d)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/ds/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * {@code GET  /ds} : get all the dS.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of dS in body.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<D>> getAllDS() {
        log.debug("REST request to get all DS");
        return dRepository.findAll().collectList();
    }

    /**
     * {@code GET  /ds} : get all the dS as a stream.
     * @return the {@link Flux} of dS.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<D> getAllDSAsStream() {
        log.debug("REST request to get all DS as a stream");
        return dRepository.findAll();
    }

    /**
     * {@code GET  /ds/:id} : get the "id" d.
     *
     * @param id the id of the d to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the d, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<D>> getD(@PathVariable("id") Long id) {
        log.debug("REST request to get D : {}", id);
        Mono<D> d = dRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(d);
    }

    /**
     * {@code DELETE  /ds/:id} : delete the "id" d.
     *
     * @param id the id of the d to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteD(@PathVariable("id") Long id) {
        log.debug("REST request to delete D : {}", id);
        return dRepository
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
