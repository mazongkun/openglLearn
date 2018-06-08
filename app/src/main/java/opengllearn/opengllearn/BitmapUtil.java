package opengllearn.opengllearn;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * The BitmapUtil apply a series of methods to create bitmap or change bitmap
 * shape or scale bitmap or mirror bitmap or save the bitmap~~;
 * 
 * @author SenseTime
 * 
 */
public class BitmapUtil {

	private static Matrix mMatrix = new Matrix();
	private static final String TAG = "BitmapUtil";
	private static final boolean DEBUG = false;

	private static Matrix getMatrix() {
		mMatrix.reset();
		return mMatrix;
	}
	
	public static void converYUVtoJPG(byte[] data, String outName, int width, int height,String dir) {
		if (data.length == 0) {
			Log.d(TAG, "data is null!");
			return;
		}
		YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		yuvImage.compressToJpeg(new Rect(0,0, width, height), 100, bos);
		String outJpgName = outName.replace("yuv", "jpg");
		try {
			File save = new File(dir);
			if (!save.exists()) {
				save.mkdir();
			}
			File file = new File(dir + outJpgName);
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(bos.toByteArray());
			fos.flush();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
 
	}

	public static Bitmap zoomBitmap(Bitmap bm, int newWidth, int newHeight, boolean recycle) {
		int width = bm.getWidth();
		int height = bm.getHeight();
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		if (scaleWidth == 1.0 && scaleHeight == 1.0) {
			return bm;
		}
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
		if (recycle && bm != null) {
			bm.recycle();
			bm = null;
		}
		return newbm;
	}

	public static Bitmap limitBitmap(Bitmap bm, int longSide, int shortSide, boolean recycle) {
        int imgW = bm.getWidth();
        int imgH = bm.getHeight();
        int imgLongSide = imgW > imgH ? imgW : imgH;
        int imgShortSide = imgW < imgH ? imgW : imgH;
        boolean isWLong = imgW > imgH;

        if (imgLongSide <= longSide && imgShortSide <= shortSide) {
            return bm;
        }
        // calculate
        if ((1.0 * imgLongSide / imgShortSide) > (1.0 * longSide / shortSide)) {
            // scale as long side
            imgShortSide = (int) (1.0 * imgShortSide * longSide / imgLongSide);
            imgLongSide = longSide;
        } else {
            // scale as short side
            imgLongSide = (int) (1.0 * imgLongSide * shortSide / imgShortSide);
            imgShortSide = shortSide;
        }
        // set new size
        if (isWLong) {
            imgW = imgLongSide;
            imgH = imgShortSide;
        } else {
            imgW = imgShortSide;
            imgH = imgLongSide;
        }

        return zoomBitmap(bm, imgW, imgH, recycle);
    }

	/**
	 * create bitmap by path
	 * 
	 * @param path
	 * @return Bitmap
	 */
	public static Bitmap createBitmapFromPath(String path) {
		File imgFile = new File(path);
		if (imgFile.exists()) {
			Bitmap bmp = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
			return bmp;
		} else {
			printLog("  createBitmapFromPath imgFile is not exist");
		}
		return null;
	}

    public static Bitmap createBitmapFromBytes(byte[] imageBytes) {
        if (imageBytes != null || imageBytes.length != 0) {
            Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            return bmp;
        } else {
            printLog("  createBitmapFromPath imgFile is not exist");
        }
        return null;
    }

	/**
	 * scale bitmap which parameter x or y is 1080
	 * 
	 * @param data
	 * @return
	 */
	public static Bitmap createScaleBitmapFromData(byte[] data, int width, int height) {
		if (data == null || data.length == 0) {
			printLog("createScaleBitmapFromData data==null||data.length==0");
			return null;
		}
		Options opts = new Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(data, 0, data.length, opts);
		int scaleX = opts.outWidth / width;
		int scaleY = opts.outHeight / height;
		int scale = scaleX > scaleY ? scaleX : scaleY;
		if (scale < 1)
			scale = 1;
		opts.inJustDecodeBounds = false;
		opts.inSampleSize = scale;
		return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
	}

	/**
	 * create bitmap by resources but the density will same as resources
	 * 
	 * @param resources
	 * @param id
	 * @return
	 */
	public static Bitmap creatBitmapFromRawResource(Resources resources, int id) {
		TypedValue value = new TypedValue();
		resources.openRawResource(id, value);
		Options opts = new Options();
		opts.inTargetDensity = value.density;
		printLog("  creatBitmapFromResource value.density=" + value.density);

		return BitmapFactory.decodeResource(resources, id, opts);
	}

	/**
	 * create source bitmap by Resource which size is specified(thumb nail
	 * bitmap)
	 * 
	 * @param id
	 *            Resources id
	 * @param width
	 *            bitmap width
	 * @param height
	 *            bitmap height
	 * @return Bitmap
	 */
	public static Bitmap createImageThumbnail(Resources resources, int id, int width, int height) {
		Bitmap bitmap = null;
		Options options = new Options();
		options.inJustDecodeBounds = true;
		bitmap = BitmapFactory.decodeResource(resources, id);
		int h = options.outHeight;
		int w = options.outWidth;
		int beWidth = w / width;
		int beHeight = h / height;
		int be = 1;
		if (beWidth < beHeight) {
			be = beWidth;
		} else {
			be = beHeight;
		}
		if (be <= 0) {
			be = 1;
		}
		options.inJustDecodeBounds = false;
		options.inSampleSize = be;
		BitmapFactory.decodeResource(resources, id, options);
		bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
		return bitmap;
	}

	/**
	 * load source bitmap by image path which size is specified
	 * 
	 * @param imagePath
	 *            image imagePath
	 * @param width
	 *            bitmap width
	 * @param height
	 *            bitmap height
	 * @return Bitmap
	 */
	public static Bitmap createImageThumbnail(String imagePath, int width, int height) {
		try {
			if (TextUtils.isEmpty(imagePath)) {
				printLog("createImageThumbnail imagePath cannot be empty");
				return null;
			}
			Bitmap bitmap = null;
			Options options = new Options();
			options.inJustDecodeBounds = true;
			bitmap = BitmapFactory.decodeFile(imagePath, options);
			int h = options.outHeight;
			int w = options.outWidth;
			int beWidth = w / width;
			int beHeight = h / height;
			int be = 1;
			if (beWidth < beHeight) {
				be = beWidth;
			} else {
				be = beHeight;
			}
			if (be <= 0) {
				be = 1;
			}
			options.inJustDecodeBounds = false;
			options.inSampleSize = be;
			bitmap = BitmapFactory.decodeStream(new FileInputStream(imagePath), null, options);
			// bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
			// ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
			return bitmap;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

    public static Bitmap createImageThumbnail(byte[] imageBytes, int width, int height) {
        if (imageBytes == null || imageBytes.length == 0) {
            printLog("createImageThumbnail imageBytes cannot be empty");
            return null;
        }

	    try {
            Bitmap bitmap = null;
            Options options = new Options();
            options.inJustDecodeBounds = true;
            int h = options.outHeight;
            int w = options.outWidth;
            int beWidth = w / width;
            int beHeight = h / height;
            int be = 1;
            if (beWidth < beHeight) {
                be = beWidth;
            } else {
                be = beHeight;
            }
            if (be <= 0) {
                be = 1;
            }
            options.inJustDecodeBounds = false;
            options.inSampleSize = be;
            bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
            // bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
            // ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            return bitmap;
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            return null;
        }
    }

	/**
	 * Create reflection images
	 * 
	 * @param bitmap
	 * @return Bitmap
	 */
	public static Bitmap createReflectionImage(Bitmap bitmap) {
		if (isEmty(bitmap)) {
			printLog("createReflectionImageWithOrigin bitmap is null");
			return null;
		}
		final int reflectionGap = 4;
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		Matrix matrix = new Matrix();
		matrix.preScale(1, -1);
		Bitmap reflectionImage = Bitmap.createBitmap(bitmap, 0, h / 2, w, h / 2, matrix, false);
		Bitmap bitmapWithReflection = Bitmap.createBitmap(w, (h + h / 2), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmapWithReflection);
		canvas.drawBitmap(bitmap, 0, 0, null);
		Paint deafalutPaint = new Paint();
		canvas.drawRect(0, h, w, h + reflectionGap, deafalutPaint);
		canvas.drawBitmap(reflectionImage, 0, h + reflectionGap, null);
		Paint paint = new Paint();
		LinearGradient shader = new LinearGradient(0, bitmap.getHeight(), 0, bitmapWithReflection.getHeight()
				+ reflectionGap, 0x70ffffff, 0x00ffffff, TileMode.CLAMP);
		paint.setShader(shader);
		paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
		canvas.drawRect(0, h, w, bitmapWithReflection.getHeight() + reflectionGap, paint);
		return bitmapWithReflection;
	}

	/**
	 * 压缩图片
	 */
	public static Bitmap compressImageFromFile(String srcPath, boolean isCompress) {
		int angle = getPictureDegree(srcPath);
		Options options = new Options();
		if (isCompress) {
			options.inJustDecodeBounds = true;// 只读边,不读内容
			BitmapFactory.decodeFile(srcPath, options);
			int sampleSize = 1;
			int w = options.outWidth;
			int h = options.outHeight;
			if (w < 0 || h < 0) { // 不是图片文件直接返回
				return null;
			}
			int requestH = 1000; // 需求最大高度
			int requestW = 1000; // 需求最大宽度
			while ((h / sampleSize > requestH) || (w / sampleSize > requestW)) {
				sampleSize = sampleSize << 1;
			}
			options.inJustDecodeBounds = false; // 不再只读边
			options.inSampleSize = sampleSize;// 设置采样率大小
		}
		Bitmap bitmap = BitmapFactory.decodeFile(srcPath, options);
		bitmap = rotateBitmap(bitmap, angle, true);
		return bitmap;
	}

	/**
	 * 压缩图片
	 */
	public static Bitmap compressImageFromFile(String srcPath, int width, int height) {
		int angle = getPictureDegree(srcPath);
		Options options = new Options();
		options.inJustDecodeBounds = true;// 只读边,不读内容
		BitmapFactory.decodeFile(srcPath, options);
		int sampleSize = 1;
		int w = options.outWidth;
		int h = options.outHeight;
		if (w < 0 || h < 0) { // 不是图片文件直接返回
			return null;
		}
		int requestH = width; // 需求最大高度
		int requestW = height; // 需求最大宽度
		while ((h / sampleSize > requestH) || (w / sampleSize > requestW)) {
			sampleSize = sampleSize << 1;
		}
		options.inJustDecodeBounds = false; // 不再只读边
		options.inSampleSize = sampleSize;// 设置采样率大小
		Bitmap bitmap = BitmapFactory.decodeFile(srcPath, options);
		bitmap = rotateBitmap(bitmap, angle, true);
		return bitmap;
	}

	/**
	 * 旋转图片
	 * 
	 * @param angle
	 * @param bitmap
	 * @return Bitmap
	 */
	public static Bitmap rotateBitmap(Bitmap bitmap, int angle, boolean recycle) {
		if (angle == 0) {
			return bitmap;
		}
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		if (recycle) {
			bitmap.recycle();
			bitmap = null;
		}
		return bmp;
	}

	/**
	 * Decode sourcePath file,and save it to outFilePath after rotated and
	 * scaled;
	 * 
	 * @param sourcePath
	 * @param outFilePath
	 */
	public static void compressImage(String sourcePath, String outFilePath) {
		if (TextUtils.isEmpty(sourcePath)) {
			printLog("compressImage srcPath cannot be null");
			return;
		}
		int degree = getPictureDegree(sourcePath);
		final int DEF_MIN = 480;
		final int DEF_MAX = 1280;
		Options newOpts = new Options();
		newOpts.inJustDecodeBounds = true;
		Bitmap bitmap;
		BitmapFactory.decodeFile(sourcePath, newOpts);
		int picMin = Math.min(newOpts.outWidth, newOpts.outHeight);
		int picMax = Math.max(newOpts.outWidth, newOpts.outHeight);
		float picShap = 1.0f * picMin / picMax;
		float defShap = 1.0f * DEF_MIN / DEF_MAX;
		float scale = 1f;
		if (picMax < DEF_MAX) {

		} else if (picShap > defShap) {
			scale = 1.0f * DEF_MAX / picMax;

		} else if (picMin > DEF_MIN) {
			scale = 1.0f * DEF_MIN / picMin;
		}
		int scaledWidth = (int) (scale * newOpts.outWidth);
		int scaledHeight = (int) (scale * newOpts.outHeight);
		newOpts.inSampleSize = (int) ((scale == 1 ? 1 : 2) / scale);
		newOpts.inJustDecodeBounds = false;
		bitmap = BitmapFactory.decodeFile(sourcePath, newOpts);
		if (scale != 1) {
			Matrix matrix = new Matrix();
			scale = (float) scaledWidth / bitmap.getWidth();
			matrix.setScale(scale, scale);
			Bitmap bitmapNew = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
			bitmap.recycle();
			bitmap = bitmapNew;
		}
		int quality;
		if (scaledHeight * scaledWidth > (1280 * 720)) {
			quality = 30;
		} else {
			quality = 80 - (int) ((float) scaledHeight * scaledWidth / (1280 * 720) * 50);
		}
		try {
			bitmap = getRotateBitmap(bitmap, degree);
			bitmap.compress(CompressFormat.JPEG, quality, new FileOutputStream(new File(outFilePath)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		bitmap.recycle();
		System.gc();
	}

	/**
	 * Used to calculate inSimpleSize value of BitmapFactpry;
	 * 
	 * @param options
	 * @param reqWidth
	 * @param reqHeight
	 * @return int
	 */
	public static int calculateInSampleSize(Options options, int reqWidth, int reqHeight) {
		if (reqWidth == 0 || reqHeight == 0) {
			return 1;
		}
		final int height = options.outHeight;
		final int width = options.outWidth;
		Log.d(TAG, "origin, w= " + width + " h=" + height);
		int inSampleSize = 1;
		if (height > reqHeight || width > reqWidth) {
			final int halfHeight = height / 2;
			final int halfWidth = width / 2;
			while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
				inSampleSize *= 2;
			}
		}
		printLog("sampleSize:" + inSampleSize);
		return inSampleSize;
	}

	/**
	 * Used to load bitmap from path
	 * 
	 * @param path
	 * @param reqWidth
	 * @param reqHeight
	 * @return Bitmap
	 */
	public static Bitmap decodeSampledBitmapFromPath(String path, int reqWidth, int reqHeight) {
		try {
			final Options options = new Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(path, options);
			options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
			options.inJustDecodeBounds = false;
			return BitmapFactory.decodeStream(new FileInputStream(path), null, options);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Used to load bitmap from resource
	 * 
	 * @param res
	 * @param resId
	 * @param reqWidth
	 * @param reqHeight
	 * @return Bitmap
	 */
	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
		final Options options = new Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
	}

	public static Bitmap decodeResource(Resources resources, int id) {
		TypedValue value = new TypedValue();
		resources.openRawResource(id, value);
		Options opts = new Options();
		opts.inTargetDensity = value.density;
		return BitmapFactory.decodeResource(resources, id, opts);
	}

	/**
	 * Used to get bitmap with uri
	 * 
	 * @param imageUri
	 * @param context
	 * @param imageWidth
	 * @return Bitmap
	 */
	public static Bitmap getBitmapFromUri(Uri imageUri, Context context, int imageWidth) {
		Bitmap bitmap = null;
		try {
			ContentResolver resolver = context.getContentResolver();
			InputStream is = resolver.openInputStream(imageUri);
			bitmap = BitmapFactory.decodeStream(is);
			int width = bitmap.getWidth(), height = bitmap.getHeight();
			int dstWidth, dstHeight;
			if (width > height) {
				dstWidth = imageWidth;
				dstHeight = imageWidth * height / width;
			} else {
				dstHeight = imageWidth;
				dstWidth = imageWidth * width / height;
			}
			bitmap = Bitmap.createScaledBitmap(bitmap, dstWidth, dstHeight, true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return bitmap;
	}

	/**
	 * The Drawable resource files into Bitmap images
	 *
	 * @param drawable
	 * @return Bitmap
	 */
	public static Bitmap getBitmapWithDrawable(Drawable drawable) {
		if (drawable == null) {
			printLog("getBitmapWithDrawable drawable is null");
			return null;
		}
		if (drawable instanceof BitmapDrawable)
			return ((BitmapDrawable) drawable).getBitmap();

		Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
				Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}

	/**
	 * crop capture the middle part of the bitmap
	 * 
	 * @param sourceBitmap
	 *            sourceBitmap
	 * @param edgeLength
	 *            Hope to get part of the side length of square,To a minimum
	 *            length is edgeLength bitmap
	 * @return Bitmap
	 */
	public static Bitmap getCenterSquareScaleBitmap(Bitmap sourceBitmap, int edgeLength) {
		if (null == sourceBitmap || edgeLength <= 0) {
			printLog("getCenterSquareScaleBitmap sourceBitmap is null or edgeLength<=0");
			return null;
		}
		Bitmap result = sourceBitmap;
		int widthOrg = sourceBitmap.getWidth();
		int heightOrg = sourceBitmap.getHeight();
		if (widthOrg > edgeLength && heightOrg > edgeLength) {
			int longerEdge = (int) (edgeLength * Math.max(widthOrg, heightOrg) / Math.min(widthOrg, heightOrg));
			int scaledWidth = widthOrg > heightOrg ? longerEdge : edgeLength;
			int scaledHeight = widthOrg > heightOrg ? edgeLength : longerEdge;
			Bitmap scaledBitmap;
			try {
				scaledBitmap = Bitmap.createScaledBitmap(sourceBitmap, scaledWidth, scaledHeight, true);
				int xTopLeft = (scaledWidth - edgeLength) / 2;
				int yTopLeft = (scaledHeight - edgeLength) / 2;
				result = Bitmap.createBitmap(scaledBitmap, xTopLeft, yTopLeft, edgeLength, edgeLength);
				scaledBitmap.recycle();
			} catch (Exception e) {
				return null;
			}
		}
		return result;
	}

	/**
	 * Used to get a round shape image,the Effect of bigger bitmap will be more
	 * obvious
	 * 
	 * @param sourceBitmap
	 * @return Bitmap
	 */
	public static Bitmap getRoundBitmap(Bitmap sourceBitmap) {
		if (isEmty(sourceBitmap)) {
			printLog("getRoundBitmap sourceBitmap is null");
			return null;
		}
		int width = sourceBitmap.getWidth();
		int height = sourceBitmap.getHeight();
		float roundPx;
		float left, top, right, bottom, dst_left, dst_top, dst_right, dst_bottom;
		if (width <= height) {
			roundPx = width / 2 - 5;
			top = 0;
			bottom = width;
			left = 0;
			right = width;
			height = width;
			dst_left = 0;
			dst_top = 0;
			dst_right = width;
			dst_bottom = width;
		} else {
			roundPx = height / 2 - 5;
			float clip = (width - height) / 2;
			left = clip;
			right = width - clip;
			top = 0;
			bottom = height;
			width = height;
			dst_left = 0;
			dst_top = 0;
			dst_right = height;
			dst_bottom = height;
		}
		Bitmap output = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect src = new Rect((int) left, (int) top, (int) right, (int) bottom);
		final Rect dst = new Rect((int) dst_left, (int) dst_top, (int) dst_right, (int) dst_bottom);
		final RectF rectF = new RectF(dst_left + 3, dst_top + 3, dst_right - 3, dst_bottom - 3);
		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(sourceBitmap, src, dst, paint);
		return output;
	}

	/**
	 * Used to get a round bitmap,you can set range(faceRect),and border
	 * color(color)
	 * 
	 * @param sourceBitmap
	 * @param faceRect
	 * @param color
	 * @return Bitmap
	 */
	public static Bitmap getRoundedCornerBitmap(Bitmap sourceBitmap, Rect faceRect, int color) {
		if (isEmty(sourceBitmap)) {
			printLog("getRoundedCornerBitmap sourceBitmap is null");
			return null;
		}
		Bitmap outBitmap = Bitmap.createBitmap(sourceBitmap.getWidth(), sourceBitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(outBitmap);
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight());
		final RectF rectF = new RectF(rect);
		final float roundPX = (faceRect.right - faceRect.left) / 2.0f;
		final float roundPY = (faceRect.bottom - faceRect.top) / 2.0f;
		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPX, roundPY, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(sourceBitmap, rect, rect, paint);
		return outBitmap;
	}

	/**
	 * Used to crop a square bitmap
	 * 
	 * @param sourceBitmap
	 * @return Bitmap
	 */
	public static Bitmap getCropBitmap(Bitmap sourceBitmap) {
		if (isEmty(sourceBitmap)) {
			printLog("getCropBitmap sourceBitmap is null");
			return null;
		}
		int w = sourceBitmap.getWidth();
		int h = sourceBitmap.getHeight();
		int wh = w > h ? h : w;
		int retX = w > h ? (w - h) / 2 : 0;
		int retY = w > h ? 0 : (h - w) / 2;
		return Bitmap.createBitmap(sourceBitmap, retX, retY, wh, wh, null, false);
	}

	/**
	 * Used to crop bitmap by define range(orginRect)
	 * 
	 * @param sourceBitmap
	 * @param orginRect
	 * @return Bitmap
	 */
	public static Bitmap getCropBitmap(Bitmap sourceBitmap, Rect orginRect) {
		if (isEmty(sourceBitmap)) {
			printLog("getCropBitmap sourceBitmap is null");
			return null;
		}
		return getCropBitmap(sourceBitmap, orginRect, 2, 2);
	}

	public static Bitmap getCropBitmap(Bitmap sourceBitmap, Rect orginRect, float scaleX, float scaleY) {
		if (sourceBitmap == null || sourceBitmap.isRecycled()) {
			return null;
		}
		Rect rect = getScaleRect(orginRect, scaleX, scaleY, sourceBitmap.getWidth(), sourceBitmap.getHeight());
		return Bitmap.createBitmap(sourceBitmap, rect.left, rect.top, rect.width(), rect.height());
	}

	/**
	 * Used to get rect which is scaled;
	 * 
	 * @param rect
	 * @param scaleX
	 * @param scaleY
	 * @param maxW
	 * @param maxH
	 * @return Rect
	 */
	public static Rect getScaleRect(Rect rect, float scaleX, float scaleY, int maxW, int maxH) {
		Rect resultRect = new Rect();
		int left = (int) (rect.left - rect.width() * (scaleX - 1) / 2);
		int right = (int) (rect.right + rect.width() * (scaleX - 1) / 2);
		int bottom = (int) (rect.bottom + rect.height() * (scaleY - 1) / 2);
		int top = (int) (rect.top - rect.height() * (scaleY - 1) / 2);
		resultRect.left = left > 0 ? left : 0;
		resultRect.right = right > maxW ? maxW : right;
		resultRect.bottom = bottom > maxH ? maxH : bottom;
		resultRect.top = top > 0 ? top : 0;
		return resultRect;
	}

	/**
	 * Used to scale bitmap ,you can print a scale value(float);
	 * 
	 * @param sourceBitmap
	 * @param scale
	 * @return Bitmap
	 */
	public static Bitmap getScaleBitmap(Bitmap sourceBitmap, float scale) {
		if (isEmty(sourceBitmap)) {
			printLog("getScaleBitmap sourceBitmap is null");
			return null;
		}
		return getScaleBitmap(sourceBitmap, scale, true);
	}

	private static Bitmap getScaleBitmap(Bitmap bitmap, float scale, boolean recycle) {
		if (scale == 1f) {
			printLog("getScaleBitmap scale == 1f");
			return bitmap;
		}
		Matrix matrix = getMatrix();
		matrix.setScale(scale, scale);
		Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		if (recycle && bitmap != null && !bitmap.equals(bmp) && !bitmap.isRecycled()) {
			bitmap.recycle();
			bitmap = null;
		}
		return bmp;
	}

	/**
	 * Used to scale bitmap by defineWidth and defineHeight;
	 * 
	 * @param sourceBitmap
	 * @param defineWidth
	 * @param defineHeight
	 * @return Bitmap
	 */
	public static Bitmap getScaleBitmap(Bitmap sourceBitmap, int defineWidth, int defineHeight) {
		if (isEmty(sourceBitmap)) {
			printLog("getCropBitmap sourceBitmap is null");
			return null;
		}
		int w = sourceBitmap.getWidth();
		int h = sourceBitmap.getHeight();
		float sw = (float) defineWidth / w;
		float sh = (float) defineHeight / h;
		float scale = sw < sh ? sw : sh;
		return getScaleBitmap(sourceBitmap, scale);
	}

	/**
	 * Used to get bitmap size which you want;
	 * 
	 * @param bitmap
	 * @param size
	 * @return Bitmap
	 */
	public static Bitmap getResizeBitmap(Bitmap bitmap, int size) {
		if (isEmty(bitmap)) {
			printLog("getResizeBitmap bitmap is null");
			return null;
		}
		double maxSize = size * 1e6;
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		float ratio = (float) Math.sqrt(maxSize / (w * h));
		if (ratio >= 1) {
			return bitmap;
		}
		return BitmapUtil.getRotateAndScaleBitmap(bitmap, 0, ratio);
	}

	/**
	 * return Media image orientation
	 * 
	 * @param context
	 * @param photoUri
	 * @return int
	 */
	public static int getOrientationFromMedia(Context context, Uri photoUri) {
		String[] imgs = { MediaStore.Images.Media.ORIENTATION };
		Cursor cursor = context.getContentResolver().query(photoUri, imgs, null, null, null);
		cursor.moveToFirst();
		int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION);
		int roate = cursor.getInt(index);
		try {
			cursor.close();
		} catch (Exception e) {

		}
		return roate;
	}

	/**
	 * Used to rotate bitmap by rotateDegree which you print
	 * 
	 * @param bitmap
	 * @param rotateDegree
	 * @return Bitmap
	 */
	public static Bitmap getRotateBitmap(Bitmap bitmap, float rotateDegree) {
		if (isEmty(bitmap)) {
			printLog("getRotateBitmap bitmap is null");
			return null;
		}
		Matrix matrix = new Matrix();
		matrix.postRotate((float) rotateDegree);
		Bitmap rotaBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
		return rotaBitmap;
	}

	/**
	 * Used to rotate and scale bitmap
	 * 
	 * @param angle
	 * @param bitmap
	 * @return Bitmap
	 */
	public static Bitmap getRotateAndScaleBitmap(Bitmap bitmap, int angle, float scale) {
		if (isEmty(bitmap)) {
			printLog("getRotateAndScaleBitmap bitmap is null");
			return null;
		}
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		matrix.postScale(scale, scale);
		Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		return resizedBitmap;
	}

	/**
	 * Used to get the image degree;
	 * 
	 * @param path
	 * @return degree
	 */
	public static int getPictureDegree(String path) {
		int degree = 0;
		try {
			ExifInterface exifInterface = new ExifInterface(path);
			int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);
			Log.d(TAG, "orientation=" + orientation);
			switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				degree = 90;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				degree = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				degree = 270;
				break;
			case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
				degree = 0;
				break;
			case ExifInterface.ORIENTATION_FLIP_VERTICAL:
				degree = 180;
				break;
			case ExifInterface.ORIENTATION_TRANSPOSE:
				degree = 90;
				break;
			case ExifInterface.ORIENTATION_TRANSVERSE:
				degree = 270;
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return degree;
	}

	/**
	 * Return a MirrorHorizontal Bitmap
	 * 
	 * @param sourceBitmap
	 * 
	 * @return Bitmap
	 */
	public static Bitmap getMirrorHorizontalBitmap(Bitmap sourceBitmap) {
		if (isEmty(sourceBitmap)) {
			printLog("getMirrorHorizontalBitmap sourceBitmap is null");
			return null;
		}
		return getMirrorHorizontalBitmap(sourceBitmap, true);
	}

	private static Bitmap getMirrorHorizontalBitmap(Bitmap sourceBitmap, boolean recycle) {
		if (isEmty(sourceBitmap)) {
			printLog("getMirrorHorizontalBitmap sourceBitmap is null");
			return null;
		}
		Matrix matrix = getMatrix();
		matrix.setScale(-1, 1);
		matrix.postTranslate(sourceBitmap.getWidth(), 0);
		Bitmap bmp = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix,
				true);
		if (recycle && sourceBitmap != null && !sourceBitmap.isRecycled()) {
			sourceBitmap.recycle();
			sourceBitmap = null;
		}
		return bmp;
	}

	public static Bitmap getRotateBitmap(Bitmap sourceBitmap, int degrees, boolean isFrontCamera, boolean recycle) {
		if (isEmty(sourceBitmap)) {
			printLog("getMirrorHorizontalBitmap sourceBitmap is null");
			return null;
		}
		if (degrees == 0 && !isFrontCamera) {
			return sourceBitmap;
		}
		Matrix matrix = getMatrix();
		matrix.postRotate(degrees);
		if (isFrontCamera) {
			matrix.postScale(-1, 1);
		}
		Bitmap bmp = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix,
				true);
		if (recycle) {
			sourceBitmap.recycle();
			sourceBitmap = null;
		}
		return bmp;
	}

	/**
	 * Return a mirrorVertical Bitmap
	 * 
	 * @param sourceBitmap
	 * @return Bitmap
	 */
	public static Bitmap getMirrorVerticalBitmap(Bitmap sourceBitmap) {
		if (isEmty(sourceBitmap)) {
			printLog("getMirrorVerticalBitmap sourceBitmap is null");
			return null;
		}
		return getMirrorVerticalBitmap(sourceBitmap, true);
	}

	private static Bitmap getMirrorVerticalBitmap(Bitmap sourceBitmap, boolean recycle) {
		if (isEmty(sourceBitmap)) {
			printLog("getMirrorVerticalBitmap sourceBitmap is null");
			return null;
		}
		Matrix matrix = getMatrix();
		matrix.setScale(1, -1);
		matrix.postTranslate(0, sourceBitmap.getHeight());
		Bitmap bmp = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix,
				true);
		if (recycle && sourceBitmap != null && !sourceBitmap.equals(bmp) && !sourceBitmap.isRecycled()) {
			sourceBitmap.recycle();
			sourceBitmap = null;
		}
		return bmp;
	}

	/**
	 * Used to draw a bitmap(size/2) which get from resource into the empty
	 * bitmap
	 * 
	 * @param sourceBitmap
	 * @param resourceId
	 * @return Bitmap
	 */
	public static Bitmap getScreenshotBitmap(Context context, Bitmap sourceBitmap, int resourceId) {
		if (isEmty(sourceBitmap)) {
			printLog("getScreenshotBitmap bitmap is null");
			return null;
		}
		Bitmap screenshot = Bitmap.createBitmap(sourceBitmap.getWidth(), sourceBitmap.getHeight(),
				Config.ARGB_8888);
		Bitmap content = BitmapFactory.decodeResource(context.getResources(), resourceId);
		Canvas canvas = new Canvas(screenshot);
		canvas.drawBitmap(content, (sourceBitmap.getWidth() - content.getWidth()) / 2,
				(sourceBitmap.getHeight() - content.getHeight()) / 2, new Paint());
		canvas.drawBitmap(sourceBitmap, 0, 0, new Paint());
		canvas.save();
		canvas.restore();
		return screenshot;
	}

	/**
	 * Used to save image(data) to savePath ,at the same time,you can print
	 * angle to rotate image and save it;
	 * 
	 * @param savePath
	 * @param data
	 * @param angle
	 */
	public static void saveJpegData(String savePath, byte[] data, int angle) {
		if (data == null || data.length == 0) {
			printLog("saveJpegData data==null||data.length==0");
			return;
		}
		try {
			Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
			Matrix matrix = new Matrix();
			matrix.reset();
			matrix.postRotate(angle);
			bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
			File file = new File(savePath);
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
			bmp.compress(CompressFormat.JPEG, 100, bos);
			bos.flush();
			bos.close();
			recycle(bmp);
			data = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Used to save bitmap to storage
	 * 
	 * @param savePath
	 * @param bitmap
	 * @return boolean
	 */
	public static boolean saveBitmapJPG(String savePath, Bitmap bitmap) {
		if (isEmty(bitmap)) {
			printLog("saveBitmapToStorage bitmap is null");
			return false;
		}
		File saveFile = new File(savePath);
		if (!saveFile.getParentFile().exists() || saveFile.getParentFile().isFile()) {
			saveFile.getParentFile().mkdirs();
		}
		FileOutputStream fileoutputstream = null;
		try {
			fileoutputstream = new FileOutputStream(savePath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		bitmap.compress(CompressFormat.JPEG, 100, fileoutputstream);
		try {
			if (null != fileoutputstream) {
				fileoutputstream.close();
			}
			Log.d(TAG, "saveImageToStorage success");
			return true;
		} catch (Exception exception4) {
			Log.e(TAG, "saveImageToStorage error");
			return false;
		}
	}

	/**
	 * Used to save bitmap to storage
	 *
	 * @param savePath
	 * @param bitmap
	 * @return boolean
	 */
	public static boolean saveBitmapPNG(String savePath, Bitmap bitmap) {
		if (isEmty(bitmap)) {
			printLog("saveBitmapPNG bitmap is null");
			return false;
		}
		File saveFile = new File(savePath);
		if (!saveFile.getParentFile().exists() || saveFile.getParentFile().isFile()) {
			saveFile.getParentFile().mkdirs();
		}
		FileOutputStream fileoutputstream = null;
		try {
			fileoutputstream = new FileOutputStream(savePath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		bitmap.compress(CompressFormat.PNG, 100, fileoutputstream);
		try {
			if (null != fileoutputstream) {
				fileoutputstream.close();
			}
			Log.d(TAG, "saveBitmapPNG success");
			return true;
		} catch (Exception exception4) {
			Log.e(TAG, "saveBitmapPNG error");
			return false;
		}
	}

	/**
	 * Used to save bitmap to define directory,at the same time ,update it to
	 * the mobile phone gallery;
	 * 
	 * @param context
	 * @param bmp
	 * @param absPath
	 */
	public static void saveImageToGallery(Context context, Bitmap bmp, String absPath) {
		if (isEmty(bmp)) {
			printLog("saveImageToGallery bitmap is null");
			return;
		}
		File appDir = new File(absPath);
		if (!appDir.exists() && !appDir.isDirectory()) {
			appDir.mkdir();
		}
		String fileName = "IMAGE_" + System.currentTimeMillis() + ".jpg";
		File file = new File(appDir, fileName);
		try {
			FileOutputStream fos = new FileOutputStream(file);
			bmp.compress(CompressFormat.JPEG, 100, fos);
			fos.flush();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			MediaStore.Images.Media.insertImage(context.getContentResolver(), file.getAbsolutePath(), fileName, null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(absPath
				+ fileName))));
	}

	/**
	 * Used to save image to MediaStore
	 * 
	 * @param imagePath
	 * @param date
	 * @param orientation
	 * @param size
	 * @param location
	 * @param contentresolver
	 * @return Uri
	 */
	public static Uri savetImageToMediaStore(String imagePath, long date, int orientation, long size,
			Location location, ContentResolver contentresolver) {
		String fileName = imagePath.substring(imagePath.lastIndexOf("/"), imagePath.lastIndexOf("."));
		ContentValues contentvalues = new ContentValues(9);
		contentvalues.put("title", fileName);
		contentvalues.put("_display_name", (new StringBuilder()).append(fileName).append(".jpg").toString());
		contentvalues.put("datetaken", Long.valueOf(date));
		contentvalues.put("mime_type", "image/jpeg");
		contentvalues.put("orientation", Integer.valueOf(orientation));
		contentvalues.put("_data", imagePath);
		contentvalues.put("_size", Long.valueOf(size));
		if (location != null) {
			contentvalues.put("latitude", Double.valueOf(location.getLatitude()));
			contentvalues.put("longitude", Double.valueOf(location.getLongitude()));
		}
		Uri uri = contentresolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentvalues);
		if (uri == null) {
			Log.e(TAG, "Failed to write MediaStore");
			uri = null;
		}
		return uri;
	}

	/**
	 * Used to recycle bitmap
	 * 
	 * @param bitmap
	 */
	public static void recycle(Bitmap bitmap) {
		if (null == bitmap || bitmap.isRecycled()) {
			return;
		}
		bitmap.recycle();
		bitmap = null;
	}

	/**
	 * whether bitmap is null or is recycled
	 * 
	 * @param bitmap
	 * @return boolean
	 */
	public static boolean isEmty(Bitmap bitmap) {
		if (bitmap == null || bitmap.isRecycled()) {
			return true;
		}
		return false;
	}

	/**
	 * print log
	 * 
	 * @param logStr
	 */
	public static void printLog(String logStr) {
		if (DEBUG) {
			Log.d(TAG, logStr);
		}
	}

    public static Bitmap getAlphaBitmap(String path) {
        File imgFile = new File(path);
        if (imgFile.exists()) {
            Options options = new Options();
            options.inPreferredConfig = Config.ALPHA_8;
            return BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
        } else {
            printLog("  getAlphaBitmap imgFile is not exist");
        }
        return null;
    }

    public static byte[] getAlphaBitmapBuffer(Bitmap bmp) {
        if (bmp == null)
            return null;

        if (bmp.getRowBytes() != bmp.getWidth()) {
            return getGrayBufferFromBitmap(bmp);
        } else {
            return getRGBAFromBitmap(bmp);
        }
    }

	public static byte[] getRGBAFromBitmap(Bitmap bitmap) {
		ByteBuffer bitmapBuffer = ByteBuffer.allocate(bitmap.getHeight() * bitmap.getRowBytes());
		bitmapBuffer.rewind();
		bitmap.copyPixelsToBuffer(bitmapBuffer);
		return bitmapBuffer.array();
	}
	public static Bitmap getBitmapFromRGBA(byte[] data, int width, int height) {
		Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(data));
		return bitmap;
	}

    public static Bitmap getBitmapFromGray(byte[] data, int width, int height) {
        if(data == null || width <= 0 || height <= 0)
            return null;
        //使用8位来保存图片
        Bitmap bitmap = Bitmap
                .createBitmap(width, height, Config.ARGB_8888);
        int pixels[] = new int[width * height];
        for (int i = 0; i < pixels.length; ++i) {
            //关键代码，生产灰度图
            pixels[i] = data[i] * 256 * 256 + data[i] * 256 + data[i] + 0xFF000000;
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        data = null;
        pixels = null;
        return bitmap;
    }

    public static byte[] getBGRFromBitmap(Bitmap bitmap) {
        // calculate how many bytes our image consists of
        int bytes = bitmap.getByteCount();

        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        bitmap.copyPixelsToBuffer(buffer); // Move the byte data to the buffer

        byte[] temp = buffer.array(); // Get the underlying array containing the data.

        byte[] pixels = new byte[(temp.length/4) * 3]; // Allocate for BGR

        // Copy pixels into place
        for (int i = 0; i < temp.length/4; i++) {

            pixels[i * 3] = temp[i * 4 + 2];        //B
            pixels[i * 3 + 1] = temp[i * 4 + 1];    //G
            pixels[i * 3 + 2] = temp[i * 4 ];       //R

        }
        return pixels;
    }

    public static Bitmap getBitmapFromBGR(byte[] bgrBuf, int width, int height) {
        if (bgrBuf == null || bgrBuf.length % 3 != 0)
            return null;

        byte[] rgbaBuf = new byte[bgrBuf.length / 3 * 4];
        for (int i=0; i<bgrBuf.length/3; i++) {
            rgbaBuf[i*4]     = bgrBuf[i*3 + 2];
            rgbaBuf[i*4 + 1] = bgrBuf[i*3 + 1];
            rgbaBuf[i*4 + 2] = bgrBuf[i*3];
            rgbaBuf[i*4 + 3] = (byte) 255;
        }
        return BitmapUtil.getBitmapFromRGBA(rgbaBuf, width, height);
    }

    public static byte[] getRGBAFromBGR(byte[] bgr, int width, int height) {
		if (bgr == null || bgr.length % 3 != 0)
			return null;

		byte[] rgba = new byte[width * height * 4];
		for (int i=0; i<bgr.length/3; i++) {
			rgba[i*4]   = bgr[i*3+2];
			rgba[i*4+1] = bgr[i*3+1];
			rgba[i*4+2] = bgr[i*3];
			rgba[i*4+3] = (byte) 0xff;
		}

		return rgba;
	}

	public static byte[] getGrayBufferFromBitmap(Bitmap bm) {
		int iBytes = bm.getWidth() * bm.getHeight();
		byte[] res = new byte[iBytes];
		Config format = bm.getConfig();
		if (format == Config.ARGB_8888) {
			ByteBuffer buffer = ByteBuffer.allocate(iBytes * 4);
			bm.copyPixelsToBuffer(buffer);
			byte[] arr = buffer.array();
			for (int i = 0; i < iBytes; i++) {
				int A, R, G, B;
				R = (int) (arr[i * 4 + 0]) & 0xff;
				G = (int) (arr[i * 4 + 1]) & 0xff;
				B = (int) (arr[i * 4 + 2]) & 0xff;
				// A=arr[i*4+3];
				byte r = (byte) (0.2989 * R + 0.5870 * G + 0.1140 * B);
				res[i] = r;
			}
		}
		if (format == Config.RGB_565) {
			ByteBuffer buffer = ByteBuffer.allocate(iBytes * 2);
			bm.copyPixelsToBuffer(buffer);
			byte[] arr = buffer.array();
			for (int i = 0; i < iBytes; i++) {
				float A, R, G, B;
				R = ((arr[i * 2 + 0] & 0xF8));
				G = ((arr[i * 2 + 0] & 0x7) << 5) + ((arr[i * 2 + 1] & 0xE0) >> 5);
				B = ((arr[i * 2 + 1] & 0x1F) << 3);
				byte r = (byte) (0.2989 * R + 0.5870 * G + 0.1140 * B);
				res[i] = r;
			}
		}
		return res;
	}

    /**
     * 将bitmap里得到的argb数据转成yuv420sp格式
     * 这个yuv420sp数据就可以直接传给MediaCodec,通过AvcEncoder间接进行编码
     * @param yuv420sp 用来存放yuv429sp数据
     * @param rgba 传入rgba数据
     * @param width   图片width
     * @param height 图片height
     */
    public static void encodeYUV420SP(byte[] yuv420sp, byte[] rgba, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = rgba[index+3] & 0xFF; // a is not used obviously
                R = rgba[index] & 0xFF;
                G = rgba[index+1] & 0xFF;
                B = rgba[index+2] & 0xFF;

                // well known RGB to YUV algorithm
                Y = ( (  66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
                U = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
                V = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % (2*4) == 0) {
                    yuv420sp[uvIndex++] = (byte)((V<0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte)((U<0) ? 0 : ((U > 255) ? 255 : U));
                }

                index += 4;
            }
        }
    }

    public static void getBitmapRect(ImageView imageViewm, RectF outRect) {
        Drawable drawable = imageViewm.getDrawable();
        if (drawable == null)
            return;

	    Matrix matrix = imageViewm.getImageMatrix();

        outRect.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        matrix.mapRect(outRect);

        Log.e(TAG, "getBitmapRect rectF = left=" + outRect.left + ", right=" + outRect.right + ", top=" + outRect.top + ", bottom=" + outRect.bottom);
    }
}
