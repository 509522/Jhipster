package com.myapp.web.rest;

import com.myapp.domain.A;
import com.myapp.repository.ARepository;
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
 * REST controller for managing {@link com.myapp.domain.A}.
 */
@RestController
@RequestMapping("/api/as")
@Transactional
public class AResource {

    private static final Logger log = LoggerFactory.getLogger(AResource.class);

    private static final String ENTITY_NAME = "a";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ARepository aRepository;

    public AResource(ARepository aRepository) {
        this.aRepository = aRepository;
    }

    /**
     * {@code POST  /as} : Create a new a.
     *
     * @param a the a to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new a, or with status {@code 400 (Bad Request)} if the a has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public Mono<ResponseEntity<A>> createA(@RequestBody A a) throws URISyntaxException {
        log.debug("REST request to save A : {}", a);
        if (a.getId() != null) {
            throw new BadRequestAlertException("A new a cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return aRepository
            .save(a)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/as/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * {@code GET  /as} : get all the aS.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of aS in body.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<A>> getAllAS() {
        log.debug("REST request to get all AS");
        return aRepository.findAll().collectList();
    }

    /**
     * {@code GET  /as} : get all the aS as a stream.
     * @return the {@link Flux} of aS.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<A> getAllASAsStream() {
        log.debug("REST request to get all AS as a stream");
        return aRepository.findAll();
    }

    /**
     * {@code GET  /as/:id} : get the "id" a.
     *
     * @param id the id of the a to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the a, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<A>> getA(@PathVariable("id") Long id) {
        log.debug("REST request to get A : {}", id);
        Mono<A> a = aRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(a);
    }

    /**
     * {@code DELETE  /as/:id} : delete the "id" a.
     *
     * @param id the id of the a to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteA(@PathVariable("id") Long id) {
        log.debug("REST request to delete A : {}", id);
        return aRepository
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
