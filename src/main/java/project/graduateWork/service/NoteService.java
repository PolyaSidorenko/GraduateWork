package project.graduateWork.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import project.graduateWork.entity.Note;
import project.graduateWork.entity.User;
import project.graduateWork.repository.NoteRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteService {
    private final NoteRepository noteRepository;
    private final UserService userService;

    public Note createNote(Long telegramId, String title) {
        User user = userService.getOrCreateUser(telegramId);
        Note note = Note.builder()
                .title(title)
                .localDateTime(LocalDateTime.now())
                .user(user)
                .build();
        return noteRepository.save(note);
    }

    public List<Note> getNotesByUserId(Long telegramId) {
        return noteRepository.findAllByUserTelegramId(telegramId);
    }

//    public void deleteNoteById(Long noteId) {
//        noteRepository.deleteById(noteId);
//    }
}
