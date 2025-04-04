package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Rating {

    private Integer id;
    @NotBlank
    private String name;
}