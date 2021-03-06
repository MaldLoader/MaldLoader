package com.maldloader.impl.classloader;

import java.util.ArrayList;
import java.util.List;

import com.maldloader.v0.api.transformer.Buf;
import com.maldloader.v0.api.transformer.LazyDefiner;
import org.jetbrains.annotations.Nullable;

public class MultiLazyDefiner implements LazyDefiner {
	public final List<LazyDefiner> definers = new ArrayList<>();

	@Override
	public @Nullable Buf forName(String name) {
		for(LazyDefiner definer : this.definers) {
			Buf buf = definer.forName(name);
			if(buf != null) {
				return buf;
			}
		}
		return null;
	}
}
