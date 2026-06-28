package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.mymarket.domain.Item;

public interface ItemRepository {
//
//    @Query("select i from Item i "
//            + "where lower(i.title) like lower(concat('%', :search, '%')) "
//            + "or lower(i.description) like lower(concat('%', :search, '%'))")
//    Page<Item> search(@Param("search") String search, Pageable pageable);
//
//    @Query(value = "select image from items where id = :id", nativeQuery = true)
//    byte[] findImageById(@Param("id") Long id);
}
