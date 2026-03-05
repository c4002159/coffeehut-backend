package com.coffeehut.coffeehut.controller;

import com.coffeehut.coffeehut.model.Item;
import com.coffeehut.coffeehut.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "http://localhost:3000")
public class ItemController {

    @Autowired
    private ItemService itemService;

    @GetMapping
    public List<Item> getMenu() {
        return itemService.getAllItems();
    }
}
