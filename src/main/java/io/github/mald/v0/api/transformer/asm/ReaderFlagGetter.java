package io.github.mald.v0.api.transformer.asm;

import org.objectweb.asm.ClassWriter;

public interface ReaderFlagGetter {
	int getClassWriterFlags(ClassHeader header);

	/**
	 * Using these will greatly slow load times as every class's maxes and/or frames will have to be recomputed
	 */
	enum StaticAsmFlags implements ReaderFlagGetter {
		MAXES(ClassWriter.COMPUTE_MAXS),
		FRAMES(ClassWriter.COMPUTE_FRAMES);

		final int maxes;

		StaticAsmFlags(int maxes) {this.maxes = maxes;}

		@Override
		public int getClassWriterFlags(ClassHeader header) {
			return 0;
		}
	}
}
