package io.github.mald.impl.classloader;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import io.github.mald.impl.LoaderPluginLoader;
import io.github.mald.v0.api.modloader.ModLoader;
import io.github.mald.v0.api.plugin.LoaderPlugin;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class Main implements Opcodes {
	public static void main(String[] args) throws Throwable {
		String path = System.clearProperty(LoaderPluginLoader.MALD + ".loaderDirectory");
		if(path == null) {
			path = LoaderPluginLoader.MALD + "_plugins";
		}
		List<Path> loaderPlugins = new ArrayList<>();
		File dir = new File(path);
		if(dir.exists() && dir.isDirectory()) {
			for(File file : Objects.requireNonNull(dir.listFiles())) {
				loaderPlugins.add(file.toPath());
			}
		}

		String plugins = System.clearProperty(LoaderPluginLoader.MALD + ".loaderPluginJars");
		if(plugins != null) {
			for(String s : plugins.split(",")) {
				loaderPlugins.add(Paths.get(s));
			}
		}

		launch(loaderPlugins, args);
	}

	public static void launch(List<Path> loaderPlugins, String[] args) throws Throwable {
		MainClassLoaderImpl[] main = {null};
		Method method = loadFromFile(loaderPlugins, main);

		// a big hack to minimize the stacktrace size
		ClassWriter writer = new ClassWriter(0);
		String[] interfaces = new String[] {"java/util/function/Consumer"};
		writer.visit(V1_6, ACC_PUBLIC, LoaderPluginLoader.MALD + "/Launcher_Generated", null, "java/lang/Object", interfaces);
		MethodVisitor visitor = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		visitor.visitVarInsn(ALOAD, 0);
		visitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		visitor.visitInsn(RETURN);
		visitor.visitMaxs(1, 1);
		visitor.visitEnd();

		MethodVisitor run = writer.visitMethod(ACC_PUBLIC, "accept", "(Ljava/lang/Object;)V", null, null);
		run.visitVarInsn(ALOAD, 1);
		run.visitTypeInsn(CHECKCAST, "[Ljava/lang/String;");
		run.visitMethodInsn(
				INVOKESTATIC,
				Type.getInternalName(method.getDeclaringClass()),
				method.getName(),
				Type.getMethodDescriptor(method),
				false);
		run.visitInsn(RETURN);
		run.visitMaxs(1, 2);
		run.visitEnd();

		byte code[] = writer.toByteArray();
		String name = LoaderPluginLoader.MALD + ".Launcher_Generated";
		Class<?> type = main[0].define(name, code, 0, code.length);
		Thread thread = Thread.currentThread();
		thread.setContextClassLoader(main[0]);
		Consumer<String[]> consumer = (Consumer<String[]>) type.newInstance();
		consumer.accept(args);
	}

	public static Method loadFromFile(List<Path> loaderPlugins, MainClassLoaderImpl[] ref) throws Throwable {
		LoaderPluginLoader impl = new LoaderPluginLoader(loaderPlugins);
		List<LoaderPlugin> plugins = impl.init(Main.class.getClassLoader());

		List<ModLoader<?>> loaders = new ArrayList<>();
		for(LoaderPlugin plugin : plugins) {
			plugin.offerModLoaders(loaders::add);
		}

		MainClassLoaderImpl main = new MainClassLoaderImpl();
		for(ModLoader<?> loader : loaders) {
			loader.init(main);
		}

		Map<String, Class<?>> mainClasses = new HashMap<>();
		for(LoaderPlugin plugin : plugins) {
			plugin.offerMainClasses(main, (id, cls) -> {
				if(!mainClasses.containsKey(id)) {
					mainClasses.put(id, cls);
				} else {
					System.err.println("Multiple main classes for " + cls);
				}
			});
		}

		String property = System.getProperty(LoaderPluginLoader.MALD + ".main");
		if(property == null) {
			throw new IllegalStateException("no main class property set! " + mainClasses.keySet());
		}
		Class<?> cls = mainClasses.get(property);
		if(cls == null) {
			throw new IllegalStateException("no main class for '" + property + "'");
		}
		ref[0] = main;
		return cls.getDeclaredMethod("main", String[].class);
	}
}