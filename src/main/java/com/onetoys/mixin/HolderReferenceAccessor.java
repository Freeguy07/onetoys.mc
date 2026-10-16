package com.onetoys.mixin;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;

@Mixin(Holder.Reference.class)
public interface HolderReferenceAccessor<T> {
    @Invoker("bindTags")
    void Onetoys$bindTags(Collection<TagKey<T>> tags);
}
