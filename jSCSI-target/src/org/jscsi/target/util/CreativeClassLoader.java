package org.jscsi.target.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jscsi.target.task.TargetTaskLibrary;

public class CreativeClassLoader extends ClassLoader {

	/** The Log interface. */
	private static final Log LOGGER = LogFactory
			.getLog(FileSystemClassLoader.class);

	private static final Map<String, Class<?>> additionalLoadedClasses = new ConcurrentHashMap<String, Class<?>>();

	private static final Set<URL> additionalClassPaths = new HashSet<URL>();

	private CreativeClassLoader() {
		this(Thread.currentThread().getContextClassLoader());
	}

	private CreativeClassLoader(ClassLoader parent) {
		super(parent);
	}

	public final void addSource(File directory) {
		try {
			additionalClassPaths.add(directory.toURI().toURL());
		} catch (MalformedURLException e) {
			logDebug("Couldn't parse file to url");
		}
	}

	public final void addSource(URL directory) {
		additionalClassPaths.add(directory);
	}

	/**
	 * Checks if location b is sub-directory of location a. a is directory and b
	 * is sub-directory. This needs a static String representing system of
	 * locations like URL.
	 * 
	 * @param directory
	 *            parent-directory
	 * @param subdirectory
	 *            sub-directory
	 * @return true if b is subdirectory of a
	 */
	private boolean isSubdirectory(String directory, String subdirectory) {
		if (directory.length() > subdirectory.length()) {
			return false;
		}
		String equalTest = subdirectory.substring(0, directory.length());
		if (directory.compareTo(equalTest) == 0) {
			return true;
		}
		return false;
	}

	/**
	 * Returns the all current system-class paths.
	 * 
	 * @return all URLs from java.class.path
	 */
	public static Set<URL> getSystemClassPaths() {
		Set<URL> classPaths = new HashSet<URL>();
		System.out.println(System.getProperty("java.class.path"));
		for (String path : System.getProperty("java.class.path", ".")
				.split(";")) {
			try {
				classPaths.add(new File(path).toURI().toURL());
			} catch (MalformedURLException e) {
				logDebug("Couldn't create URL of String \"" + path + "\": "
						+ e.getMessage());
			}
		}
		return classPaths;
	}

	/**
	 * Takes a URL and tries to convert it into a package name. The Precondition
	 * is that the URL must link to a class file that is a sub-directory of all
	 * used class paths.
	 * 
	 * @param classURL
	 *            URL to a class file
	 * @return package name or null
	 */
	private String urlToPackageName(URL classURL) {
		// take every system and loaded additional clathpath to check
		Set<URL> classPaths = new HashSet<URL>();
		classPaths.addAll(getSystemClassPaths());
		classPaths.addAll(additionalClassPaths);
		Iterator<URL> pathIterator = classPaths.iterator();
		URL testedFile = null;
		String result = null;
		// if sub-directory of classPath, return package name
		while (pathIterator.hasNext()) {
			testedFile = pathIterator.next();
			if (isSubdirectory(testedFile.getPath(), classURL.getPath())) {
				result = classURL.getPath().substring(
						testedFile.getPath().length(),
						classURL.getPath().length());
				result = result.replace(".class", "");
				result = result.replace("/", ".");
			}
		}
		return result;
	}
	
	

	/**
	 * coverts a absolte class name to the classes simple name.
	 * I.e. "org.package.name.class" will return "class.name".
	 * Will return null if no conversion is possible.
	 * @param packageName
	 * @return
	 */
	private String packageNameToClassName(String packageName) {
		String result = null;
		try {
			result = packageName.substring(packageName.lastIndexOf('.') + 1,
					packageName.length());
		} catch (Exception e) {
			logDebug("Couldn't parse package name to class name");
		}
		return result;
	}

	private boolean isFullPackageName(String name) {
		// a package name with a dot cannot be smaller than 3
		if (name.length() < 3) {
			return false;
		}
		// there must be at least one dot in between first and last character
		if ((name.lastIndexOf('.') > 1)
				&& (name.lastIndexOf('.') < name.length() - 1)) {
			// if no absolute class name, so no "Class.class"
			if (!isAbsoluteClassName(name)) {
				return true;
			}
		}
		return false;
	}

	public Class<?> loadClass(URL url) throws MalformedURLException,
			CreativeClassLoaderException {
		InputStream fileInput = null;
		byte[] code = null;
		try {
			fileInput = url.openConnection().getInputStream();
			code = new byte[fileInput.available()];
			fileInput.read(code);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return defineAndLoadClass(code);
	}

	public final Class<?> defineAndLoadClass(byte[] code)
			throws CreativeClassLoaderException {
		Class<?> loadedClass = null;
		loadedClass = defineClass(null, code, 0, code.length);
		try {
			loadedClass = loadClass(loadedClass.getName());
		} catch (ClassNotFoundException e) {
			throw new CreativeClassLoaderException("Could");
		}
		logTrace("Defined and loaded new class: " + loadedClass.getName());
		return loadedClass;
	}

//	public Class<?> loadClassUsingEverythingYouHave(String name) {
//		Class<?> loadedClass = null;
//		try {
//			loadedClass = loadClass(name);
//		} catch (ClassNotFoundException e) {
//			for(URL url : additionalClassPaths){
//				//loadAllClasses(null, , recursive)
//			}
//		}
//	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return super.loadClass(name);
	}

	public Set<Class<?>> loadAllClasses(Set<Class<?>> loadedClasses, File file,
			boolean recursive) {
		if (loadedClasses == null) {
			loadedClasses = new HashSet<Class<?>>();
		}
		for (File listedFile : file.listFiles()) {
			if (file.isFile()) {
				try {
					loadedClasses.add(loadClass(file.toURI().toURL()));
				} catch (Exception e) {
					logTrace(e.getMessage());
				}

				return loadedClasses;
			}
			if (listedFile.isDirectory() && recursive) {
				loadAllClasses(loadedClasses, listedFile, true);
			}

		}
		return loadedClasses;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		return super.findClass(name);
	}

	private boolean isAbsoluteClassName(String name) {
		if (name.endsWith(".class")) {
			return true;
		}
		return false;
	}

	public static CreativeClassLoader getInstance() {
		if (!Singleton.hasInstance(TargetTaskLibrary.class)) {
			Singleton.setInstance(new CreativeClassLoader());
		}
		CreativeClassLoader instance = null;
		try {
			instance = Singleton.getInstance(CreativeClassLoader.class);
		} catch (ClassNotFoundException e) {
			throw new Error("Couldn't load instance of "
					+ CreativeClassLoader.class);
		}
		return instance;
	}

	/**
	 * Logs a trace Message, if trace log is enabled within the logging
	 * environment.
	 * 
	 * @param logMessage
	 */
	private static void logTrace(String logMessage) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(" Message: " + logMessage);
		}
	}

	/**
	 * Logs a debug Message, if debug log is enabled within the logging
	 * environment.
	 * 
	 * @param logMessage
	 */
	private static void logDebug(String logMessage) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.trace(" Message: " + logMessage);

		}
	}
	
	

}