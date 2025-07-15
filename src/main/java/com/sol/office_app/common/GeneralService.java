package com.sol.office_app.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface GeneralService<T, ID> {
    Page<T> findAll(Pageable pageable);

    Page<T> search(String query, Pageable pageable);

    Optional<T> get(ID id);

    Optional<T> save(T entity);

    Optional<T> update(ID id, T entity);

    Optional<T> delete(ID id, T entity);
}
