package com.example.demo.mongodb;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Document(collection = "foo_bar")
public class FooBar {
    @Id
    private String id;

    private String name;

    private String description;
}
