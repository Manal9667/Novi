package com.novi.shelf;

import com.novi.entity.Shelf;
import com.novi.entity.User;
import com.novi.exception.DuplicateResourceException;
import com.novi.exception.ResourceNotFoundException;
import com.novi.repository.ShelfBookRepository;
import com.novi.repository.ShelfRepository;
import com.novi.service.BookService;
import com.novi.service.ShelfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShelfServiceTest {

    @Mock private ShelfRepository shelfRepository;
    @Mock private ShelfBookRepository shelfBookRepository;
    @Mock private BookService bookService;

    @InjectMocks
    private ShelfService shelfService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("reader").build();
    }

    @Test
    void create_withUniqueName_savesShelf() {
        when(shelfRepository.findByUserAndNameIgnoreCase(user, "Favorites")).thenReturn(Optional.empty());
        Shelf saved = Shelf.builder().id(1L).user(user).name("Favorites").build();
        when(shelfRepository.save(any(Shelf.class))).thenReturn(saved);
        when(shelfBookRepository.findByShelf(saved)).thenReturn(Collections.emptyList());

        var response = shelfService.create(user, "Favorites");

        assertThat(response.name()).isEqualTo("Favorites");
    }

    @Test
    void create_withDuplicateName_throwsDuplicate() {
        when(shelfRepository.findByUserAndNameIgnoreCase(user, "Favorites"))
                .thenReturn(Optional.of(new Shelf()));

        assertThatThrownBy(() -> shelfService.create(user, "Favorites"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void delete_shelfNotOwnedByUser_throwsNotFound() {
        when(shelfRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shelfService.delete(user, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
