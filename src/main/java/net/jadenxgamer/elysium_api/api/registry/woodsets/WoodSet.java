
package net.jadenxgamer.elysium_api.api.registry.woodsets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * WoodSet
 */
public interface WoodSet {

  public List<RegistryObject<Block>> getBlockObjects();

  public Optional<RegistryObject<Block>> getBlock(WoodBlockType type);

  public RegistryObject<Block> getBlockUnsafe(WoodBlockType type);

  public List<RegistryObject<Item>> getItemObjects();

  public Optional<RegistryObject<Item>> getItem(WoodBlockType type);

  public RegistryObject<Item> getItemUnsafe(WoodBlockType type);

  public static class Builder {

    private String name;

    private final List<WoodBlockType> blockTypes = new ArrayList<>();
    private final Map<WoodBlockType, Block> templates = new HashMap<>();
    private final Map<WoodBlockType, Function<String, String>> nameModifiers = new HashMap<>();
    private final Map<WoodBlockType, Consumer<Properties>> propertiesModifiers = new HashMap<>();
    private final Set<WoodBlockType> noItem = new HashSet<>();    

    private WoodType woodType;

    private Consumer<Properties> propertiesModifier = p -> {};

    public Builder(String name, WoodType woodType) {
      this.name = name;
      this.woodType = woodType;
    }

    public static Builder forWood(String name, WoodType woodType) {
      return new Builder(name, woodType);
    }

    public Builder addTypes(List<Pair<WoodBlockType, Block>> pairs) {
      pairs.forEach(type -> {
        this.blockTypes.add(type.getFirst());
        this.templates.put(type.getFirst(), type.getSecond());
      });
      return this;
    }

    public Builder removeTypes(WoodBlockType... blockTypes) {
      for (WoodBlockType blockType : blockTypes) {
        this.blockTypes.remove(blockType);
      }
      return this;
    }

    public Builder withNameModifier(WoodBlockType blockType, Function<String, String> modifier) {
      nameModifiers.put(blockType, modifier);
      return this;
    }

    public Builder withPropertiesModifier(WoodBlockType type, Consumer<Properties> modifier) {
      propertiesModifiers.put(type, modifier);
      return this;
    }

    public Builder noItem(WoodBlockType... blockTypes) {
      for (WoodBlockType blockType : blockTypes) {
        noItem.add(blockType);
      }
      return this;
    }

    public WoodSet build(DeferredRegister<Block> blockRegister, DeferredRegister<Item> itemRegister) {
      if (this.blockTypes.contains(WoodBlockType.STAIRS) && !this.blockTypes.contains(WoodBlockType.PLANKS)) {
        throw new IllegalStateException("Due to Mojang's implementation, registering STAIRS requires registering PLANKS!");
      }

      Map<WoodBlockType, RegistryObject<Block>> blocks = new HashMap<>();
      Map<WoodBlockType, RegistryObject<Item>> items = new HashMap<>();

      if (blockTypes.contains(WoodBlockType.PLANKS)) {
        register(WoodBlockType.PLANKS, blocks, blockRegister, items, itemRegister);
      }
    
      blockTypes.forEach(type -> {
        if (type != WoodBlockType.PLANKS) {
          register(type, blocks, blockRegister, items, itemRegister);
        }
      });

      return new WoodSet() {

      	@Override
      	public List<RegistryObject<Block>> getBlockObjects() {
      	  return List.copyOf(blocks.values());
      	}

      	@Override
      	public Optional<RegistryObject<Block>> getBlock(WoodBlockType type) {
      	  return Optional.ofNullable(getBlockUnsafe(type));
      	}

      	@Override
      	public RegistryObject<Block> getBlockUnsafe(WoodBlockType type) {
      	  return blocks.get(type);
      	}

      	@Override
      	public List<RegistryObject<Item>> getItemObjects() {
      	  return List.copyOf(items.values());
      	}

      	@Override
      	public Optional<RegistryObject<Item>> getItem(WoodBlockType type) {
      	  return Optional.ofNullable(getItemUnsafe(type));
      	}

      	@Override
      	public RegistryObject<Item> getItemUnsafe(WoodBlockType type) {
      	  return items.get(type);
      	}
        
      };
      
    }

    public void register(WoodBlockType type, Map<WoodBlockType, RegistryObject<Block>> blocks, DeferredRegister<Block> blockRegister, Map<WoodBlockType, RegistryObject<Item>> items, DeferredRegister<Item> itemRegister) {
        String id = nameModifiers.getOrDefault(type, s -> s).apply(type.getName(name));
        RegistryObject<Block> block = blockRegister.register(id, () -> {
          Properties properties = Properties.copy(templates.get(type));
          propertiesModifier.accept(properties);
          propertiesModifiers.getOrDefault(type, t -> {}).accept(properties);
          return type.make(properties, woodType, Optional.ofNullable(blocks.get(WoodBlockType.PLANKS)));
        });
        if (!noItem.contains(type)) {
          items.put(type, itemRegister.register(id, () -> new BlockItem(block.get(), new Item.Properties())));
        }
        blocks.put(type, block);
    }
  }
  
}
