package com.maldloader.v0.api.transformer.asm;

import org.objectweb.asm.ClassVisitor;

/**
 * A transformer that uses ClassVisitor instead of ClassNode
 */
public interface ClassVisitorTransformer {
	/**
	 * @see ClassVisitor#ClassVisitor(int, ClassVisitor)
	 */
	ClassVisitor accept(ClassHeader header, ClassVisitor visitor);
}
