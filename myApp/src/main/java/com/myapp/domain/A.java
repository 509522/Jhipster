package com.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A A.
 */
@Table("a")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class A implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    @Transient
    @JsonIgnoreProperties(value = { "a" }, allowSetters = true)
    private Set<B> bs = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public A id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<B> getBs() {
        return this.bs;
    }

    public void setBs(Set<B> bs) {
        if (this.bs != null) {
            this.bs.forEach(i -> i.setA(null));
        }
        if (bs != null) {
            bs.forEach(i -> i.setA(this));
        }
        this.bs = bs;
    }

    public A bs(Set<B> bs) {
        this.setBs(bs);
        return this;
    }

    public A addB(B b) {
        this.bs.add(b);
        b.setA(this);
        return this;
    }

    public A removeB(B b) {
        this.bs.remove(b);
        b.setA(null);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof A)) {
            return false;
        }
        return getId() != null && getId().equals(((A) o).getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "A{" +
            "id=" + getId() +
            "}";
    }
}