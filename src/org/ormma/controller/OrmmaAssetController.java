/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.ormma.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.ormma.view.OrmmaView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.widget.Toast;
import de.guj.ems.mobile.sdk.util.SdkLog;

/**
 * The Class OrmmaAssetController. This class handles asset management for orrma
 */
public class OrmmaAssetController extends OrmmaController {

	private final static String SdkLog_TAG = "OrmmaAssetController";

	//public final static float WEBVIEW_VIEWPORT_SCALE = Screen.getScreenWidth() / 320.0f;
	
	//private final static byte [] WEBVIEW_VIEWPORT_META = ("<meta name='viewport' content='target-densitydpi=device-dpi, width=320, user-scalable=no, initial-scale=" + WEBVIEW_VIEWPORT_SCALE + "' />").getBytes();
	
	private final static byte [] WEBVIEW_VIEWPORT_META = ("<meta name='viewport' content='initial-scale=1.0, user-scalable=no' />").getBytes();

	private final static byte[] WEBVIEW_BODY_STYLE = "<body style=\"margin:0; padding:0; overflow:hidden; background-color:transparent;margin: 0px; padding: 0px; display:-webkit-box;-webkit-box-orient:horizontal;-webkit-box-pack:center;-webkit-box-align:center;\">"
			.getBytes();

	class FileComparatorByDate implements Comparator<File> {
		@Override
		public int compare(File object1, File object2) {
			Long object1Date = object1.lastModified();
			Long object2Date = object2.lastModified();
			return object1Date.compareTo(object2Date);
		}
	}

	/**
	 * The Constant HEX_CHARS.
	 */
	private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', };

	/**
	 * Delete directory.
	 * 
	 * @param path
	 *            the path
	 * @return true, if successful
	 */
	static public boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

	/**
	 * Delete directory.
	 * 
	 * @param path
	 *            the path
	 * @return true, if successful
	 */
	static public boolean deleteDirectory(String path) {
		if (path != null)
			return deleteDirectory(new File(path));
		return false;
	}

	/**
	 * Instantiates a new ormma asset controller.
	 * 
	 * @param adView
	 *            the ad view
	 * @param c
	 *            the c
	 */
	public OrmmaAssetController(OrmmaView adView, Context c) {
		super(adView, c);
		//SdkLog.i(SdkLog_TAG, "WebView viewport scale meta was set to " + WEBVIEW_VIEWPORT_SCALE);
	}

	/**
	 * Adds an asset.
	 * 
	 * @param alias
	 *            the alias
	 * @param url
	 *            the url
	 */
	@JavascriptInterface
	public void addAsset(String url, String alias) {
		try {
			if (url.startsWith("ormma://screenshot")) {
				Activity parent = (Activity) mContext;
				Window window = parent.getWindow();
				InputStream in = getScreenshot(window.getDecorView());

				if (in != null) {
					writeAssetToDisk(in, alias);
					mOrmmaView
							.injectJavaScript("window.ormmaview.fireAssetReadyEvent('"
									+ alias + "', '" + url + "' )");
				} else {
					mOrmmaView
							.injectJavaScript("window.ormmaview.fireErrorEvent(\"addAsset\",\"Ein Screenshot konnte nicht gemacht werden.\")");
				}
			} else if (url.startsWith("ormma://photo")) {
				try {
					final String copyAlias = new String(alias);
					final String copyUrl = new String(url);
					Camera.PictureCallback jpegPictureCallback = new Camera.PictureCallback() {
						@Override
						public void onPictureTaken(byte[] data, Camera camera) {
							try {
								InputStream in = new ByteArrayInputStream(data);
								writeAssetToDisk(in, copyAlias);
								mOrmmaView
										.injectJavaScript("window.ormmaview.fireAssetReadyEvent('"
												+ copyAlias
												+ "', '"
												+ copyUrl
												+ "' )");
							} catch (Exception e) {
								mOrmmaView
										.injectJavaScript("window.ormmaview.fireErrorEvent(\"addAsset\",\"Datei wurden ich nicht im Cache abgelegt.\")");
							}
						}
					};

					Camera camera = Camera.open();
					camera.startPreview();
					Thread.sleep(1000);
					camera.takePicture(null, null, jpegPictureCallback);
				} catch (Exception e) {
					SdkLog.e(SdkLog_TAG, "Error taking photo.", e);
					mOrmmaView
							.injectJavaScript("window.ormmaview.fireErrorEvent(\"addAsset\",\"Ein Foto kann leider nicht aufgenommen werden.\")");
				}
			} else {
				HttpEntity entity = getHttpEntity(url);
				InputStream in = entity.getContent();
				writeAssetToDisk(in, alias);
				mOrmmaView
						.injectJavaScript("window.ormmaview.fireAssetReadyEvent('"
								+ alias + "', '" + url + "' )");

				try {
					entity.consumeContent();
				} catch (Exception e) {
				}
			}
		} catch (Exception e) {
			mOrmmaView
					.injectJavaScript("window.ormmaview.fireErrorEvent(\"addAsset\",\"Datei wurde nicht im Cache abgelegt.\")");
		}
	}

	private InputStream getScreenshot(View view) {
		try {
			view.setDrawingCacheEnabled(true);
			Bitmap screenshot = Bitmap.createBitmap(view.getDrawingCache());
			view.setDrawingCacheEnabled(false);

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			screenshot.compress(Bitmap.CompressFormat.PNG, 90, out);

			byte[] bs = out.toByteArray();
			return new ByteArrayInputStream(bs);
		} catch (Exception e) {
			mOrmmaView
					.injectJavaScript("window.ormmaview.fireErrorEvent(\"addAsset\",\"Ein Screenshot konnte nicht erzeugt werden.\")");
		}
		return null;
	}

	/**
	 * Builds a hex string
	 * 
	 * @param digest
	 *            the digest
	 * @return the string
	 */
	private String asHex(MessageDigest digest) {
		byte[] hash = digest.digest();
		char buf[] = new char[hash.length * 2];
		for (int i = 0, x = 0; i < hash.length; i++) {
			buf[x++] = HEX_CHARS[(hash[i] >>> 4) & 0xf];
			buf[x++] = HEX_CHARS[hash[i] & 0xf];
		}
		return new String(buf);
	}

	/**
	 * Cache remaining.
	 * 
	 * @return the cache remaining
	 */
	@JavascriptInterface
	public int cacheRemaining() {
		File filesDir = mContext.getFilesDir();
		StatFs stats = new StatFs(filesDir.getPath());
		int free = stats.getFreeBlocks() * stats.getBlockSize();
		return free;
	}

	/**
	 * Copy text file from jar into asset directory.
	 * 
	 * @param alias
	 *            the alias to store it in
	 * @param source
	 *            the source
	 * @return the path to the copied asset
	 */
	public String copyTextFromJarIntoAssetDir(String alias, String source) {
		InputStream in = null;
		try {
			URL url = OrmmaAssetController.class.getClassLoader().getResource(
					source);
			String file = url.getFile();
			if (file.startsWith("file:")) {
				file = file.substring(5);
			}
			int pos = file.indexOf("!");
			if (pos > 0)
				file = file.substring(0, pos);
			JarFile jf = new JarFile(file);
			JarEntry entry = jf.getJarEntry(source);
			in = jf.getInputStream(entry);
			String name = writeToDisk(in, alias, false);
			return name;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
					// TODO: handle exception
				}
				in = null;
			}
		}
		return null;
	}

	/**
	 * Delete old ads.
	 */
	public void deleteOldAds() {
		String filesDir = getFilesDir();
		File adDir = new File(filesDir + java.io.File.separator + "ad");
		deleteDirectory(adDir);
	}
	
	public void deleteOldAds(String localAdDir) {
		File adDir = new File(localAdDir);
		deleteDirectory(adDir);
	}

	private String getAlias(File assetFile) {
		File assetDir = getAssetDir("");
		int startAlias = assetFile.getAbsolutePath().indexOf(
				assetDir.getAbsolutePath());

		if (startAlias >= 0) {
			return assetFile.getAbsolutePath().substring(
					startAlias + assetDir.getAbsolutePath().length() + 1);
		} else {
			return null;
		}
	}

	private List<File> getAllFiles(File folder, List<File> files) {
		if (folder == null)
			return files;
		if (folder.exists() && folder.isDirectory()) {
			File[] innerFiles = folder.listFiles();
			for (File file : innerFiles) {
				if (file.isFile()) {
					files.add(file);
				} else if (file.isDirectory()) {
					getAllFiles(file, files);
				}
			}
			return files;
		} else {
			return files;
		}
	}

	private List<File> getAllFilesSortedByDate(File folder) {
		List<File> files = new Vector<File>();
		files = getAllFiles(folder, files);
		sort(files, new FileComparatorByDate());
		return files;
	}

	/**
	 * Gets the asset dir.
	 * 
	 * @param path
	 *            the path
	 * @return the asset dir
	 */
	private File getAssetDir(String path) {
		File filesDir = mContext.getFilesDir();
		File newDir = new File(filesDir.getPath() + java.io.File.separator
				+ path);
		return newDir;
	}

	/**
	 * Gets the asset name.
	 * 
	 * @param asset
	 *            the asset
	 * @return the asset name
	 */
	private String getAssetName(String asset) {
		int lastSep = asset.lastIndexOf(java.io.File.separatorChar);
		String name = asset;

		if (lastSep >= 0) {
			name = asset.substring(asset
					.lastIndexOf(java.io.File.separatorChar) + 1);
		}
		return name;
	}

	/**
	 * Gets the asset output string.
	 * 
	 * @param asset
	 *            the asset
	 * @return the asset output string
	 * @throws FileNotFoundException
	 *             the file not found exception
	 */
	public FileOutputStream getAssetOutputString(String asset)
			throws FileNotFoundException {
		File dir = getAssetDir(getAssetPath(asset));
		dir.mkdirs();
		File file = new File(dir, getAssetName(asset));
		return new FileOutputStream(file);
	}

	/**
	 * Gets the asset path.
	 * 
	 * @return the asset path
	 */
	@JavascriptInterface
	public String getAssetPath() {
		return "file://" + mContext.getFilesDir() + "/";
	}

	/**
	 * Gets the asset path.
	 * 
	 * @param asset
	 *            the asset
	 * @return the asset path
	 */
	private String getAssetPath(String asset) {
		int lastSep = asset.lastIndexOf(java.io.File.separatorChar);
		String path = "/";

		if (lastSep >= 0) {
			path = asset.substring(0,
					asset.lastIndexOf(java.io.File.separatorChar));
		}
		return path;
	}

	@JavascriptInterface
	public long getCacheRemaining() {
		File filesDir = mContext.getFilesDir();
		StatFs stats = new StatFs(filesDir.getPath());
		long free = stats.getAvailableBlocks() * stats.getBlockSize();
		return free;
	}

	/**
	 * Gets the files dir for the activity.
	 * 
	 * @return the files dir
	 */
	private String getFilesDir() {
		return mContext.getFilesDir().getPath();
	}

	/**
	 * pulls a resource from the web
	 * 
	 * @param url
	 *            the url
	 * @return the http entity
	 */
	private HttpEntity getHttpEntity(String url)
	/**
	 * get the http entity at a given url
	 */
	{
		HttpEntity entity = null;
		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(url);
			HttpResponse response = httpclient.execute(httpget);
			entity = response.getEntity();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return entity;
	}

	/**
	 * Move a file to ad directory.
	 * 
	 * @param fn
	 *            the filename
	 * @param filesDir
	 *            the files directory
	 * @param subDir
	 *            the sub directory
	 * @return the path where it was stored
	 */
	private String moveToAdDirectory(String fn, String filesDir, String subDir) {
		File file = new File(filesDir + java.io.File.separator + fn);
		File adDir = new File(filesDir + java.io.File.separator + "ad");
		adDir.mkdir();
		File dir = new File(filesDir + java.io.File.separator + "ad"
				+ java.io.File.separator + subDir);
		dir.mkdir();
		file.renameTo(new File(dir, file.getName()));

		return dir.getPath() + java.io.File.separator;
	}

	/**
	 * Removes the asset.
	 * 
	 * @param asset
	 *            the asset
	 */
	@JavascriptInterface
	public void removeAsset(String asset) {
		File dir = getAssetDir(getAssetPath(asset));
		dir.mkdirs();
		File file = new File(dir, getAssetName(asset));
		file.delete();

		String str = "window.ormmaview.fireAssetRemovedEvent('" + asset + "' )";
		mOrmmaView.injectJavaScript(str);
	}

	private boolean retireAssets(long needBytes) {
		File assetDir = getAssetDir("");

		if (getCacheRemaining() > needBytes) {
			return true;
		} else {
			List<File> assetFiles = getAllFilesSortedByDate(assetDir);
			int n = 0;
			int numberFiles = assetFiles.size();

			while ((getCacheRemaining() < needBytes) && (numberFiles > 0)
					&& (n < numberFiles)) {
				File assetFile = assetFiles.get(0);

				if (assetFile.delete()) {
					assetFiles.remove(0);
					String alias = getAlias(assetFile);

					if (alias != null) {
						mOrmmaView
								.injectJavaScript("window.ormmaview.fireAssetRetiredEvent('"
										+ alias + "' )");
					}
				}
				n++;
			}

			if (getCacheRemaining() < needBytes) {
				return false;
			} else {
				return true;
			}
		}
	}

	private <T> void sort(List<T> list, Comparator<T> c) {
		@SuppressWarnings("unchecked")
		T[] a = (T[]) list.toArray();
		Arrays.sort(a, c);
		ListIterator<T> i = list.listIterator();
		for (int j = 0; j < a.length; j++) {
			i.next();
			i.set((T) a[j]);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ormma.controller.OrmmaController#stopAllListeners()
	 */
	@Override
	public void stopAllListeners() {
		// TODO Auto-generated method stub

	}

	private Uri addToGallery(File img, String title, String name,
			String description, String dateTaken, String mimeType) {
		ContentValues image = new ContentValues();

		image.put(Images.Media.TITLE, title);
		image.put(Images.Media.DISPLAY_NAME, name);
		image.put(Images.Media.DESCRIPTION, description);
		image.put(Images.Media.DATE_ADDED, dateTaken);
		image.put(Images.Media.DATE_TAKEN, dateTaken);
		image.put(Images.Media.DATE_MODIFIED, dateTaken);
		image.put(Images.Media.MIME_TYPE, mimeType);
		image.put(Images.Media.ORIENTATION, 0);

		File parent = img.getParentFile();
		String path = parent.toString().toLowerCase(Locale.GERMAN);
		String fname = parent.getName().toLowerCase(Locale.GERMAN);
		
		image.put(Images.ImageColumns.BUCKET_ID, path.hashCode());
		image.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, fname);
		image.put(Images.Media.SIZE, img.length());

		image.put(Images.Media.DATA, img.getAbsolutePath());

		return mContext.getContentResolver().insert(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, image);
	}

	@SuppressLint("NewApi")
	@JavascriptInterface
	public void storePicture(String url) {
		try {
			HttpEntity entity = getHttpEntity(url);
			InputStream in = entity.getContent();

			File dir = Environment
					.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			dir.mkdirs();

			Header contentTypeHeader = entity.getContentType();
			String contentType = null;
			if (contentTypeHeader != null) {
				contentType = contentTypeHeader.getValue();
			}
			String fileName = URLUtil.guessFileName(url, null, contentType);
			File writeFile = new File(dir, fileName);
			writeToDisk(in, writeFile);
			addToGallery(writeFile, "Wallpaper" + System.currentTimeMillis(),
					fileName, "Werbung", "", contentType);

			try {
				entity.consumeContent();

				Toast.makeText(mContext,
						"Danke! Das Bild wurde in der Galerie abgelegt.",
						Toast.LENGTH_SHORT).show();

			} catch (Exception e) {
			}
		} catch (Exception e) {
			e.printStackTrace();
			mOrmmaView
					.injectJavaScript("window.ormmaview.fireErrorEvent(\"storePicture\",\"Bild konnte nicht in der Galerie abgelegt werden.\")");
		}
	}

	public void writeAssetToDisk(InputStream in, String file)
			throws IllegalStateException, IOException {

		byte buff[] = new byte[1024];
		FileOutputStream out = getAssetOutputString(file);

		do {
			int numread = in.read(buff);
			if (numread <= 0)
				break;

			if (!retireAssets(numread)) {
				mOrmmaView
						.injectJavaScript("window.ormmaview.fireErrorEvent(\"No free memory\", \"OrmmaAssetController\")");
				return;
			}

			out.write(buff, 0, numread);
		} while (true);

		out.flush();
		out.close();
	}

	public String writeToDisk(InputStream in, File writeFile)
			throws IllegalStateException, IOException {

		byte buff[] = new byte[1024];
		FileOutputStream out = new FileOutputStream(writeFile);

		do {
			int numread = in.read(buff);
			if (numread <= 0)
				break;

			out.write(buff, 0, numread);
		} while (true);

		out.flush();
		out.close();
		return writeFile.getAbsolutePath();
	}

	/**
	 * Write a stream to disk.
	 * 
	 * @param in
	 *            the input stream
	 * @param file
	 *            the file to store it in
	 * @param storeInHashedDirectory
	 *            use a hashed directory name
	 * @return the path where it was stired
	 * @throws IllegalStateException
	 *             the illegal state exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String writeToDisk(InputStream in, String file,
			boolean storeInHashedDirectory) throws IllegalStateException,
			IOException
	/**
	 * writes a HTTP entity to the specified filename and location on disk
	 */
	{
		byte buff[] = new byte[1024];

		MessageDigest digest = null;
		if (storeInHashedDirectory) {
			try {
				digest = java.security.MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		FileOutputStream out = null;
		try {
			out = getAssetOutputString(file);

			do {
				int numread = in.read(buff);
				if (numread <= 0)
					break;

				if (storeInHashedDirectory && digest != null) {
					digest.update(buff);
				}
				out.write(buff, 0, numread);

			} while (true);
			out.flush();
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (Exception e) {
					// TODO: handle exception
				}
				out = null;
			}
		}
		// out.close();
		// in.close();
		String filesDir = getFilesDir();

		if (storeInHashedDirectory && digest != null) {
			filesDir = moveToAdDirectory(file, filesDir, asHex(digest));
		}
		return filesDir + file;

	}

	/**
	 * Write an input stream to a file wrapping it with ormma stuff
	 * 
	 * @param in
	 *            the input stream
	 * @param file
	 *            the file to store it in
	 * @param storeInHashedDirectory
	 *            use a hashed directory name
	 * @return the path where it was stored
	 * @throws IllegalStateException
	 *             the illegal state exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String writeToDiskWrap(InputStream in, String file,
			boolean storeInHashedDirectory, String injection,
			String bridgePath, String ormmaPath) throws IllegalStateException,
			IOException
	/**
	 * writes a HTTP entity to the specified filename and location on disk
	 */
	{
		byte buff[] = new byte[1024];

		MessageDigest digest = null;
		if (storeInHashedDirectory) {
			try {
				digest = java.security.MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}

		// check for html tag in the input
		ByteArrayOutputStream fromFile = new ByteArrayOutputStream();
		FileOutputStream out = null;
		try {
			do {
				int numread = in.read(buff);

				if (numread <= 0) {
					break;
				}

				if (storeInHashedDirectory && digest != null) {
					digest.update(buff);
				}

				fromFile.write(buff, 0, numread);

			} while (true);

			String wholeHTML = fromFile.toString();
			// SdkLog.d("html",wholeHTML);
			boolean hasHTMLWrap = wholeHTML.indexOf("</html>") >= 0;

			// TODO cannot have injection when full html

			StringBuffer wholeHTMLBuffer = null;

			if (hasHTMLWrap) {
				wholeHTMLBuffer = new StringBuffer(wholeHTML);

				int start = wholeHTMLBuffer.indexOf("/ormma_bridge.js");

				if (start <= 0) {
					// TODO error
				}

				wholeHTMLBuffer.replace(start,
						start + "/ormma_bridge.js".length(), "file:/"
								+ bridgePath);

				start = wholeHTMLBuffer.indexOf("/ormma.js");

				if (start <= 0) {
					// TODO error
				}

				wholeHTMLBuffer.replace(start, start + "/ormma.js".length(),
						"file:/" + ormmaPath);
			}

			out = getAssetOutputString(file);

			if (!hasHTMLWrap) {
				out.write("<!DOCTYPE html>".getBytes());
				out.write("<html>".getBytes());
				out.write("<head>".getBytes());
				out.write(WEBVIEW_VIEWPORT_META);
				out.write("<title>-w-</title> ".getBytes());
				out.write(("<script src=\"file:/" + bridgePath + "\" type=\"text/javascript\"></script>")
						.getBytes());
				out.write(("<script src=\"file:/" + ormmaPath + "\" type=\"text/javascript\"></script>")
						.getBytes());

				if (injection != null) {
					out.write("<script type=\"text/javascript\">".getBytes());
					out.write(injection.getBytes());
					out.write("</script>".getBytes());
				}
				out.write("</head>".getBytes());
				out.write(WEBVIEW_BODY_STYLE);
				out.write("<div align=\"center\"> ".getBytes());
			}

			if (!hasHTMLWrap) {
				out.write(fromFile.toByteArray());
			} else {
				out.write(wholeHTMLBuffer.toString().getBytes());
			}

			if (!hasHTMLWrap) {
				out.write("</div> ".getBytes());
				out.write("</body> ".getBytes());
				out.write("</html> ".getBytes());
			}

			out.flush();
			// out.close();
			// in.close();
		} finally {
			if (fromFile != null) {
				try {
					fromFile.close();
				} catch (Exception e) {
					// TODO: handle exception
				}
				fromFile = null;
			}
			if (out != null) {
				try {
					out.close();
				} catch (Exception e) {
					// TODO: handle exception
				}
				out = null;
			}
		}
		String filesDir = getFilesDir();

		if (storeInHashedDirectory && digest != null) {
			filesDir = moveToAdDirectory(file, filesDir, asHex(digest));
		}
		return filesDir;
	}

	/**
	 * Write an input stream to a file wrapping it with orrma stuff
	 * 
	 * @param data
	 *            raw data
	 * @param file
	 *            the file to store it in
	 * @param storeInHashedDirectory
	 *            use a hashed directory name
	 * @return the path where it was stired
	 * @throws IllegalStateException
	 *             the illegal state exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String writeToDiskWrap(String data, String file,
			boolean storeInHashedDirectory, String injection,
			String bridgePath, String ormmaPath) throws IllegalStateException,
			IOException
	/**
	 * writes a HTTP entity to the specified filename and location on disk
	 */
	{
		MessageDigest digest = null;
		if (storeInHashedDirectory) {
			try {
				digest = java.security.MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		boolean hasHTMLWrap = data.indexOf("</html>") >= 0;
		FileOutputStream out = getAssetOutputString(file);
		
		if (!hasHTMLWrap) {
			out.write("<!DOCTYPE html>".getBytes());
			out.write("<html>".getBytes());
			out.write("<head>".getBytes());
			out.write(WEBVIEW_VIEWPORT_META);
			out.write("<title>-w-</title> ".getBytes());
			out.write(("<script src=\"file://" + bridgePath + "\" type=\"text/javascript\"></script>")
					.getBytes());
			out.write(("<script src=\"file://" + ormmaPath + "\" type=\"text/javascript\"></script>")
					.getBytes());
		}
		else {
			data = data.replace("<head>", ("<head><script src=\"file://" + bridgePath + "\" type=\"text/javascript\"></script>")
			+ ("<script src=\"file://" + ormmaPath + "\" type=\"text/javascript\"></script>"));
		}

		if (injection != null) {
			out.write("<script type=\"text/javascript\">".getBytes());
			out.write(injection.getBytes());
			out.write("</script>".getBytes());
		}
		if (!hasHTMLWrap) {
			out.write("</head>".getBytes());
			out.write(WEBVIEW_BODY_STYLE);
			out.write("<div align=\"center\" style=\"text-align: center;\"> "
					.getBytes());
		}
		out.write(data.getBytes());
		if (!hasHTMLWrap) {
			out.write("</div> ".getBytes());
			out.write("</body> ".getBytes());
			out.write("</html> ".getBytes());
		}

		out.flush();
		out.close();

		String filesDir = getFilesDir();

		if (storeInHashedDirectory && digest != null) {
			digest.update(data.getBytes());
			filesDir = moveToAdDirectory(file, filesDir, asHex(digest));
		}
		return filesDir;

	}
}
