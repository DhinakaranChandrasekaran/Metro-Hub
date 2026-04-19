package com.metrohub.repositories;

import com.metrohub.models.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    

    Optional<Department> findByName(String name);

    

    Optional<Department> findByCode(String code);

    

    List<Department> findByIsActiveTrueOrderByDisplayOrderAsc();

    

    boolean existsByName(String name);
}
