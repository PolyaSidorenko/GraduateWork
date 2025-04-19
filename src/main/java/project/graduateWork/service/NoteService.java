package project.graduateWork.service;

import lombok.RequiredArgsConstructor;
import org.jvnet.hk2.annotations.Service;
import project.graduateWork.entity.Note;
import project.graduateWork.entity.Task;
import project.graduateWork.entity.User;
import project.graduateWork.repository.NoteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NoteService {
    private final NoteRepository noteRepository;
    private final UserService userService;

    public Note createNote(Long telegramId, String title, String description) {
        User user = userService.getOrCreateUser(telegramId);
        Note note = Note.builder()
                .title(title)
                .description(description)
                .localDateTime(LocalDateTime.now())
                .user(user)
                .build();
        return noteRepository.save(note);
    }

    public List<Note> getTaskByUserId(Long telegramId) {
        return noteRepository.findAllByUserTelegramId(telegramId);
    }

    public Optional<Note> getNoteById(Long noteId) {
        return noteRepository.findById(noteId);
    }

    public void deleteNoteById(Long noteId) {
        noteRepository.deleteById(noteId);
    }
}
