package project.graduateWork.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.graduateWork.entity.Note;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
}
