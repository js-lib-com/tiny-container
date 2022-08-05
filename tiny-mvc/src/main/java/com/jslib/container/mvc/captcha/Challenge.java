package com.jslib.container.mvc.captcha;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.jslib.util.Strings;

/**
 * A challenge has a random set of images and a value related to one image from set. One from these images is the correct
 * challenge response. It is sent to client that displays images and value allowing the user to select the right image.
 * <p>
 * Implementation note: Challenge instance is loaded by client script. Fields {@link #images} and {@link #value} are known to
 * script logic by name and should not be changed.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class Challenge implements Serializable {
	/** Java serialization version. */
	private static final long serialVersionUID = -6867379696435211853L;

	/** Constant for CAPTCHA resource URL. */
	private static final String RESOURCE_URL = "captcha/image?";

	/** Random generator for images set. Selects random images from images repository provided to constructor. */
	private transient final Random randomImageGenerator = new Random();

	/**
	 * Random generator for challenge value. It is used to choose a random image from images set and compute challenge value
	 * based on image file name, see {@link #getValue(File)}.
	 */
	private transient final Random randomValueGenerator = new Random();

	/** Current challenge set of tokens mapped to related image files, from images repository given to constructor. */
	private final Map<String, File> tokenedImageFiles = new HashMap<>();

	/**
	 * Images set stored as relative URL with token request parameter. Image URL is based on {@link #RESOURCE_URL}. Every image
	 * from this challenge images set is identified by an unique URL.
	 * <p>
	 * This field is visible on client script with this specific name.
	 */
	private final List<String> images = new ArrayList<>();

	/**
	 * Challenge value. This value is displayed to user as a hint related to one of {@link #images}. This field is visible on
	 * client script with this specific name.
	 */
	private final String value;

	/**
	 * Create challenge instance and initialize it from given images repository and with requested set size. Set size is a hint
	 * and client should not rely on its value but use {@link #images} size instead.
	 * <p>
	 * Images repository should contain only images related to CAPTCHA. All files are considered candidates for challenge set.
	 * Sub-directories are not included.
	 * 
	 * @param repository images repository,
	 * @param setSize challenge images set size.
	 */
	public Challenge(File repository, int setSize) {
		// loads random images from repository
		// Random.nextInt(int) has an approximative uniform distribution and with very unlikely probability can return the same
		// image file index multiple times
		// in such cases returned images set is smaller than requested set size

		// skip sub-directories from images repository scanning
		File[] files = repository.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isFile();
			}
		});

		Set<File> filesSet = new HashSet<>(setSize);
		while (filesSet.size() < setSize) {
			filesSet.add(files[this.randomImageGenerator.nextInt(files.length)]);
		}

		this.images.clear();
		this.tokenedImageFiles.clear();
		for (File file : filesSet) {
			String token = Strings.UUID();
			this.images.add(RESOURCE_URL + token);
			this.tokenedImageFiles.put(token, file);
		}

		List<File> values = new ArrayList<>(this.tokenedImageFiles.values());
		this.value = getValue(values.get(this.randomValueGenerator.nextInt(setSize)));
	}

	/**
	 * Get image file related to given unique token.
	 * 
	 * @param token unique token, null or empty tolerated.
	 * @return image file or null if not registered token.
	 */
	public File getImage(String token) {
		return tokenedImageFiles.get(token);
	}

	/**
	 * Test that selected image identified by given token is the right response.
	 * 
	 * @param token unique token identifying selected image, null or empty tolerated.
	 * @return true if token identifies this challenge response.
	 */
	public boolean verifyResponse(String token) throws NullPointerException {
		return value.equals(getValue(tokenedImageFiles.get(token)));
	}

	/** Regular expression constant for file extension. */
	private static final String EXTENSION_REX = "\\.[^.]+$";

	/** Regular expression constant for not letters characters used as words separator. */
	private static final String NOT_LETTERS_REX = "[^a-z]+";

	/**
	 * Challenge value associated with image file. File name is converted to lower case, extension removed and non letter
	 * characters replaces with space, that is, non letters are considered words separators.
	 * 
	 * @param file image file, null tolerated.
	 * @return challenge value or null if given file is null.
	 */
	static String getValue(File file) throws NullPointerException {
		if (file == null) {
			return null;
		}
		return file.getName().toLowerCase().replaceAll(EXTENSION_REX, "").replaceAll(NOT_LETTERS_REX, " ");
	}
	
	// --------------------------------------------------------------------------------------------
	
	String value() {
		return value;
	}
	
	Map<String, File> tokenedImageFiles() {
		return tokenedImageFiles;
	}
	
	List<String> images() {
		return images;
	}
}