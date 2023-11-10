package dk.sdu.inventory_service.controller;

import dk.sdu.inventory_service.dto.InventoryDto;
import dk.sdu.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryDto> getAllProducts(){
        return inventoryService.getAllInventory();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Optional<InventoryDto> getProductById(@PathVariable String id){
        return inventoryService.getItemById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addProductToInventory (@RequestBody InventoryDto inventoryDto){
        inventoryService.addToInventory(inventoryDto);
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteProductFromInventory (@PathVariable String id){
        inventoryService.deleteFromInventory(id);
    }

    @PatchMapping("/edit/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void updateProduct(@PathVariable String id,@RequestBody InventoryDto inventoryDto){
        inventoryDto.setId(id);
        inventoryService.updateInventory(inventoryDto);
    }
}
