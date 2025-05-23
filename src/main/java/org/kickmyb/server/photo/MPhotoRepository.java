package org.kickmyb.server.photo;

import org.kickmyb.server.task.MTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MPhotoRepository extends JpaRepository<MPhoto, Long> {
    Optional<MPhoto> findByTask(MTask task);
}
