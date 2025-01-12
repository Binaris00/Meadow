package net.satisfy.meadow.item.armor;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.satisfy.meadow.registry.ArmorRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class FurHead extends ArmorItem {
    private final ResourceLocation hatTexture;

    public FurHead(Holder<ArmorMaterial> holder, Type type, Properties properties, ResourceLocation hatTexture) {
        super(holder, type, properties);
        this.hatTexture = hatTexture;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> list, TooltipFlag tooltipFlag) {
        ArmorRegistry.appendToolTip(list);
    }

    public ResourceLocation getHatTexture()
    {
        return hatTexture;
    }

    @Override
    public @NotNull EquipmentSlot getEquipmentSlot() {
        return this.type.getSlot();
    }
}