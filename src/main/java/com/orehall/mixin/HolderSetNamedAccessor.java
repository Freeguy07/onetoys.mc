package com.orehall.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;

@Mixin(HolderSet.Named.class)
public interface HolderSetNamedAccessor<T> {
    @Invoker("bind")
    void orehall$bind(List<Holder<T>> contents);
}
