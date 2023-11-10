package dk.sdu.inventory_service.service;

import dk.sdu.inventory_service.dto.InventoryDto;
import dk.sdu.inventory_service.mapper.InventoryDtoMapper;
import dk.sdu.inventory_service.model.Inventory;
import dk.sdu.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryDtoMapper inventoryDtoMapper;

    public List<InventoryDto> getAllInventory(){
        var inventory = inventoryRepository.findAll();
        return inventory.stream().map(inventoryDtoMapper).collect(Collectors.toList());
    }

    public Optional<InventoryDto> getItemById(String id){
        var inventory = inventoryRepository.findById(id);
        return inventory.map(inventoryDtoMapper);
    }

    public void addToInventory(InventoryDto inventoryDto){
        Inventory inventory = Inventory.builder()
                .name(inventoryDto.getName())
                .description(inventoryDto.getDescription())
                .price(inventoryDto.getPrice())
                .stock(inventoryDto.getStock())
                .bikeType(inventoryDto.getBikeType())
                .build();

        inventoryRepository.save(inventory);
        log.info("Saved product {} to inventory", inventory.getId());
    }

    public void deleteFromInventory(String id){
        Inventory inventory = Inventory.builder().build();
        inventoryRepository.deleteById(id);
        log.info("Product {} removed from inventory", inventory.getId());
    }

    public void updateInventory(InventoryDto inventoryDto) {
        // Validate that inventory products has a non-null and non-empty ID
        if (inventoryDto.getId() == null || inventoryDto.getId().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty for updating inventory");
        }

        // Fetch the existing entity from the database
        var inventoryById = inventoryRepository.findById(inventoryDto.getId());

        if (inventoryById.isPresent()) {
            // Update the existing entity with the values from the DTO
            Inventory existingInventory = inventoryById.get();
            existingInventory.setName(inventoryDto.getName());
            existingInventory.setDescription(inventoryDto.getDescription());
            existingInventory.setPrice(inventoryDto.getPrice());
            existingInventory.setStock(inventoryDto.getStock());
            existingInventory.setBikeType(inventoryDto.getBikeType());

            // Save the updated entity back to the database
            inventoryRepository.save(existingInventory);

            log.info("Updated product {} in inventory", existingInventory.getId());
        } else {
            throw new IllegalArgumentException("No product found with ID: " + inventoryDto.getId());
        }
    }

}
