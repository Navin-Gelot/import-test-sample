package com.example.demo.rest;

import com.example.demo.modal.*;
import com.example.demo.service.ImportDataService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class EsImport {
    private final ImportDataService importDataService;

    @GetMapping("/hello")
    public String getName() throws IOException {
        importDataService.checkEs();
        importDataService.addDummyData();
        return "hello";
    }

}
